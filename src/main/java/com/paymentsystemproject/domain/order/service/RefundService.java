package com.paymentsystemproject.domain.order.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.order.dto.CreateRefundRequestDto;
import com.paymentsystemproject.domain.order.dto.CreateRefundResponseDto;
import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.order.entity.OrderItem;
import com.paymentsystemproject.domain.order.entity.Refund;
import com.paymentsystemproject.domain.order.entity.RefundItem;
import com.paymentsystemproject.domain.order.repository.RefundItemRepository;
import com.paymentsystemproject.domain.order.repository.RefundRepository;
import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.domain.payment.service.PaymentService;
import com.paymentsystemproject.domain.point.service.PointService;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 환불 DB 처리를 담당하는 Service.
 *
 * 역할:
 * - 주문 / 결제 조회
 * - 환불 가능 여부 검증
 * - 환불 금액 계산
 * - Refund / RefundItem 저장
 * - 재고 복구
 * - 포인트 반환 / 적립 포인트 회수
 * - 결제 상태 변경
 *
 * 중요:
 * - 이 Service는 DB 트랜잭션 안에서 처리할 작업만 담당한다.
 * - 실제 PG 취소 API 호출은 Facade에서 트랜잭션 종료 후 수행한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final OrderItemService orderItemService;
    private final RefundRepository refundRepository;
    private final RefundItemRepository refundItemRepository;
    private final PointService pointService;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final RefundCalculator refundCalculator;

    /**
     * 환불 DB 처리 메인 메서드.
     *
     * 이 메서드 안의 작업은 하나의 DB 트랜잭션으로 묶인다.
     * 메서드가 정상 종료되면 DB 커밋이 완료되고,
     * Facade에서 그 다음 PG 취소 API를 호출한다.
     */
    @Transactional
    public RefundProcessResult processRefund(
        Long memberId,
        Long orderId,
        CreateRefundRequestDto requestDto
    ) {
        validateDuplicateRefundItems(requestDto);

        Payment payment = paymentService.findByOrderIdAndMemberId(orderId, memberId);
        Order order = payment.getOrder();

        paymentService.validateRefundablePayment(payment);

        List<RefundItemCalculation> calculations = createRefundItemCalculations(
            payment,
            orderId,
            requestDto
        );

        Integer totalPointRefundAmount = calculateTotalPointRefundAmount(calculations);
        Integer totalPgRefundAmount = calculateTotalPgRefundAmount(calculations);
        Integer totalRefundAmount = calculateTotalRefundAmount(calculations);

        Integer currentCancellableAmount = calculateCurrentCancellablePgAmount(
            payment,
            totalPgRefundAmount
        );

        changeRefundStatus(order, payment, totalRefundAmount);

        Refund refund = saveRefund(
            payment,
            requestDto.reason(),
            totalPointRefundAmount,
            totalPgRefundAmount
        );

        List<RefundItem> refundItems = saveRefundItems(refund, calculations);

        restoreStock(calculations);

        Member member = order.getMember();
        refundUsedPoint(member, payment, totalPointRefundAmount);
        cancelEarnedPoint(member, payment, totalRefundAmount);

        CreateRefundResponseDto responseDto = CreateRefundResponseDto.from(refund, refundItems);

        return new RefundProcessResult(
            responseDto,
            refund.getId(),
            payment.getPortonePaymentId(),
            totalPgRefundAmount,
            currentCancellableAmount,
            requestDto.reason()
        );
    }

    private void validateDuplicateRefundItems(CreateRefundRequestDto requestDto) {
        Set<Long> orderItemIds = new HashSet<>();

        for (CreateRefundRequestDto.RefundItemRequestDto itemRequestDto : requestDto.items()) {
            boolean isAdded = orderItemIds.add(itemRequestDto.orderItemId());

            if (!isAdded) {
                throw new BusinessException(ErrorCode.DUPLICATE_REFUND_ITEM);
            }
        }
    }

    /**
     * 현재 PG 취소 가능 금액을 계산한다.
     *
     * PortOne 취소 API에 넘길 currentCancellableAmount 값이다.
     */
    private Integer calculateCurrentCancellablePgAmount(
        Payment payment,
        Integer totalPgRefundAmount
    ) {
        Integer alreadyPgRefundAmount = refundRepository.sumPgRefundAmountByPaymentId(payment.getId());
        Integer currentCancellableAmount = payment.getPgAmount() - alreadyPgRefundAmount;

        if (totalPgRefundAmount > currentCancellableAmount) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return currentCancellableAmount;
    }

    /**
     * 이번 환불 이후 결제 상태를 변경한다.
     */
    private void changeRefundStatus(
        Order order,
        Payment payment,
        Integer totalRefundAmount
    ) {
        Integer alreadyRefundAmount = refundRepository.sumRefundAmountByPaymentId(payment.getId());
        Integer afterRefundAmount = alreadyRefundAmount + totalRefundAmount;

        if (afterRefundAmount > payment.getTotalAmount()) {
            throw new BusinessException(ErrorCode.EXCEED_REFUNDABLE_QUANTITY);
        }

        if (afterRefundAmount.equals(payment.getTotalAmount())) {
            paymentService.changeToRefunded(payment);
            orderService.cancelByFullRefund(order);
            return;
        }

        paymentService.changeToPartialRefundedIfCompleted(payment);
    }

    /**
     * 환불 요청 상품들을 돌면서 상품별 환불 계산 결과를 만든다.
     */
    private List<RefundItemCalculation> createRefundItemCalculations(
        Payment payment,
        Long orderId,
        CreateRefundRequestDto requestDto
    ) {
        List<RefundItemCalculation> calculations = new ArrayList<>();

        for (CreateRefundRequestDto.RefundItemRequestDto itemRequestDto : requestDto.items()) {
            OrderItem orderItem = orderItemService.findOrderItemForRefund(
                itemRequestDto.orderItemId(), orderId
            );

            orderItemService.validateRefundableQuantity(
                orderItem, itemRequestDto.quantity()
            );

            Integer itemRefundAmount = refundCalculator.calculateItemRefundAmount(orderItem.getPrice(),
                itemRequestDto.quantity());

            Integer pointRefundAmount = refundCalculator.calculatePointRefundAmount(payment, itemRefundAmount);

            Integer pgRefundAmount = refundCalculator.calculatePgRefundAmount(itemRefundAmount, pointRefundAmount);

            RefundItemCalculation calculation = new RefundItemCalculation(
                orderItem,
                itemRequestDto.quantity(),
                pointRefundAmount,
                pgRefundAmount
            );

            calculations.add(calculation);
        }

        return calculations;
    }

    /**
     * 상품별 포인트 환불 금액을 모두 더한다.
     */
    private Integer calculateTotalPointRefundAmount(List<RefundItemCalculation> calculations) {
        return calculations.stream()
            .mapToInt(RefundItemCalculation::pointRefundAmount)
            .sum();
    }

    /**
     * 상품별 PG 환불 금액을 모두 더한다.
     */
    private Integer calculateTotalPgRefundAmount(List<RefundItemCalculation> calculations) {
        return calculations.stream()
            .mapToInt(RefundItemCalculation::pgRefundAmount)
            .sum();
    }

    /**
     * 상품별 전체 환불 금액을 모두 더한다.
     */
    private Integer calculateTotalRefundAmount(List<RefundItemCalculation> calculations) {
        return calculations.stream()
            .mapToInt(RefundItemCalculation::totalRefundAmount)
            .sum();
    }

    /**
     * 환불 전체 기록을 저장한다.
     */
    private Refund saveRefund(
        Payment payment,
        String reason,
        Integer totalPointRefundAmount,
        Integer totalPgRefundAmount
    ) {
        Refund refund = Refund.createCompletedRefund(
            payment,
            reason,
            totalPointRefundAmount,
            totalPgRefundAmount
        );

        return refundRepository.save(refund);
    }

    /**
     * 환불 상품 상세 기록들을 저장한다.
     */
    private List<RefundItem> saveRefundItems(
        Refund refund,
        List<RefundItemCalculation> calculations
    ) {
        List<RefundItem> refundItems = new ArrayList<>();

        for (RefundItemCalculation calculation : calculations) {
            RefundItem refundItem = RefundItem.createRefundItem(
                refund,
                calculation.orderItem(),
                calculation.quantity(),
                calculation.pointRefundAmount(),
                calculation.pgRefundAmount()
            );

            RefundItem savedRefundItem = refundItemRepository.save(refundItem);
            refundItems.add(savedRefundItem);
        }

        return refundItems;
    }

    /**
     * 환불된 상품 수량만큼 상품 재고를 복구한다.
     */
    private void restoreStock(List<RefundItemCalculation> calculations) {
        List<OrderItemService.OrderItemRefundStockRestoreCommand> commands = calculations.stream()
            .map(calculation -> new OrderItemService.OrderItemRefundStockRestoreCommand(
                calculation.orderItem(),
                calculation.quantity()
            ))
            .toList();

        orderItemService.restoreStock(commands);
    }

    /**
     * 결제 때 사용했던 포인트를 회원에게 다시 돌려준다.
     */
    private void refundUsedPoint(
        Member member,
        Payment payment,
        Integer totalPointRefundAmount
    ) {
        pointService.refundUsedPoint(member, payment, totalPointRefundAmount);
    }

    /**
     * 결제 때 적립했던 포인트 중 환불 비율만큼 회수한다.
     */
    private void cancelEarnedPoint(
        Member member,
        Payment payment,
        Integer totalRefundAmount
    ) {
        Integer cancelEarnPoint = refundCalculator.calculateCancelEarnPoint(payment, totalRefundAmount);

        pointService.cancelEarnedPoint(member, payment, cancelEarnPoint);
    }

    /**
     * 환불 상품 1개에 대한 계산 결과를 담는 Service 내부 전용 record.
     */
    private record RefundItemCalculation(
        OrderItem orderItem,
        Integer quantity,
        Integer pointRefundAmount,
        Integer pgRefundAmount
    ) {

        /**
         * 이 상품 1개에 대한 전체 환불 금액을 반환한다.
         */
        private Integer totalRefundAmount() {
            return pointRefundAmount + pgRefundAmount;
        }
    }

    /**
     * DB 환불 처리 후 Facade로 넘길 결과.
     *
     * Facade는 이 값을 가지고 트랜잭션 밖에서 PG 취소 API를 호출한다.
     */
    public record RefundProcessResult(
        CreateRefundResponseDto responseDto,
        Long refundId,
        String portonePaymentId,
        Integer pgRefundAmount,
        Integer currentCancellableAmount,
        String reason
    ) {

        public boolean hasPgRefundAmount() {
            return pgRefundAmount != null && pgRefundAmount > 0;
        }
    }
}
