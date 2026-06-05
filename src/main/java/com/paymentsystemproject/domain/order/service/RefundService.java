package com.paymentsystemproject.domain.order.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.order.dto.CreateRefundRequestDto;
import com.paymentsystemproject.domain.order.dto.CreateRefundResponseDto;
import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.order.entity.OrderItem;
import com.paymentsystemproject.domain.order.entity.Refund;
import com.paymentsystemproject.domain.order.entity.RefundItem;
import com.paymentsystemproject.domain.order.repository.OrderItemRepository;
import com.paymentsystemproject.domain.order.repository.OrderRepository;
import com.paymentsystemproject.domain.order.repository.RefundItemRepository;
import com.paymentsystemproject.domain.order.repository.RefundRepository;
import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.domain.payment.entity.PaymentStatus;
import com.paymentsystemproject.domain.payment.repository.PaymentRepository;
import com.paymentsystemproject.domain.point.entity.PointTransaction;
import com.paymentsystemproject.domain.point.repository.PointRepository;
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

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RefundRepository refundRepository;
    private final RefundItemRepository refundItemRepository;
    private final PaymentRepository paymentRepository;
    private final PointRepository pointRepository;

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
        Order order = findOrder(orderId);
        validateOrderOwner(order, memberId);

        Payment payment = findPayment(order);
        validateRefundablePayment(payment);

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

        changePaymentStatus(payment, totalRefundAmount);

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

    /**
     * 주문 ID로 주문을 조회한다.
     */
    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    /**
     * 주문 객체로 결제 정보를 조회한다.
     */
    private Payment findPayment(Order order) {
        return paymentRepository.findByOrder(order)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    /**
     * 로그인한 회원의 주문인지 검증한다.
     */
    private void validateOrderOwner(Order order, Long memberId) {
        if (!order.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }
    }

    /**
     * 환불 가능한 결제 상태인지 검증한다.
     */
    private void validateRefundablePayment(Payment payment) {
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException(ErrorCode.ALREADY_REFUNDED);
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED
            && payment.getStatus() != PaymentStatus.PARTIAL_REFUNDED) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PAID);
        }
    }

    /**
     * 주문 상품 ID와 주문 ID로 주문 상품을 조회한다.
     *
     * orderItemId만으로 조회하지 않고 orderId도 같이 확인해서,
     * 다른 주문의 주문 상품을 환불하는 상황을 막는다.
     */
    private OrderItem findOrderItem(Long orderItemId, Long orderId) {
        return orderItemRepository.findByIdAndOrder_Id(orderItemId, orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));
    }

    /**
     * 요청한 환불 수량이 잔여 환불 가능 수량을 초과하지 않는지 검증한다.
     */
    private void validateRefundableQuantity(OrderItem orderItem, Integer requestQuantity) {
        Integer refundedQuantity = refundItemRepository.sumRefundedQuantityByOrderItemId(orderItem.getId());
        Integer refundableQuantity = orderItem.getQuantity() - refundedQuantity;

        if (requestQuantity > refundableQuantity) {
            throw new BusinessException(ErrorCode.EXCEED_REFUNDABLE_QUANTITY);
        }
    }

    /**
     * 상품 1개에 대한 환불 금액을 계산한다.
     *
     * 환불 금액 = 주문 당시 가격 * 환불 수량
     */
    private Integer calculateItemRefundAmount(OrderItem orderItem, Integer requestQuantity) {
        return orderItem.getPrice() * requestQuantity;
    }

    /**
     * 환불 금액 중 포인트로 돌려줄 금액을 계산한다.
     *
     * 현재 정책:
     * - 결제 당시 사용 포인트 비율에 따라 포인트 환불 금액을 산정한다.
     */
    private Integer calculatePointRefundAmount(Payment payment, Integer refundAmount) {
        if (payment.getTotalAmount() == 0) {
            return 0;
        }

        return refundAmount * payment.getUsePoint() / payment.getTotalAmount();
    }

    /**
     * 환불 금액 중 PG로 취소할 금액을 계산한다.
     */
    private Integer calculatePgRefundAmount(Integer refundAmount, Integer pointRefundAmount) {
        return refundAmount - pointRefundAmount;
    }

    /**
     * 환불 금액 비율에 따라 회수할 적립 포인트를 계산한다.
     */
    private Integer calculateCancelEarnPoint(Payment payment, Integer refundAmount) {
        if (payment.getTotalAmount() == 0) {
            return 0;
        }

        return refundAmount * payment.getEarnedPoint() / payment.getTotalAmount();
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
    private void changePaymentStatus(Payment payment, Integer totalRefundAmount) {
        Integer alreadyRefundedAmount = refundRepository.sumRefundAmountByPaymentId(payment.getId());
        Integer cumulativeRefundAmount = alreadyRefundedAmount + totalRefundAmount;

        if (cumulativeRefundAmount > payment.getTotalAmount()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (cumulativeRefundAmount.equals(payment.getTotalAmount())) {
            payment.changeStatus(PaymentStatus.REFUNDED);
            return;
        }

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            payment.changeStatus(PaymentStatus.PARTIAL_REFUNDED);
        }
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
            OrderItem orderItem = findOrderItem(itemRequestDto.orderItemId(), orderId);
            validateRefundableQuantity(orderItem, itemRequestDto.quantity());

            Integer itemRefundAmount = calculateItemRefundAmount(orderItem, itemRequestDto.quantity());
            Integer pointRefundAmount = calculatePointRefundAmount(payment, itemRefundAmount);
            Integer pgRefundAmount = calculatePgRefundAmount(itemRefundAmount, pointRefundAmount);

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
        for (RefundItemCalculation calculation : calculations) {
            calculation.orderItem().getProduct().increaseStock(calculation.quantity());
        }
    }

    /**
     * 결제 때 사용했던 포인트를 회원에게 다시 돌려준다.
     */
    private void refundUsedPoint(
        Member member,
        Payment payment,
        Integer totalPointRefundAmount
    ) {
        if (totalPointRefundAmount <= 0) {
            return;
        }

        member.increasePoint(totalPointRefundAmount);

        PointTransaction pointTransaction = PointTransaction.createRefundUsePoint(
            member,
            payment,
            totalPointRefundAmount
        );

        pointRepository.save(pointTransaction);
    }

    /**
     * 결제 때 적립했던 포인트 중 환불 비율만큼 회수한다.
     */
    private void cancelEarnedPoint(
        Member member,
        Payment payment,
        Integer totalRefundAmount
    ) {
        Integer cancelEarnPoint = calculateCancelEarnPoint(payment, totalRefundAmount);

        if (cancelEarnPoint <= 0) {
            return;
        }

        member.decreasePoint(cancelEarnPoint);

        PointTransaction pointTransaction = PointTransaction.createCancelEarnPoint(
            member,
            payment,
            cancelEarnPoint
        );

        pointRepository.save(pointTransaction);
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
