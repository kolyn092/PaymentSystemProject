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
 * 환불 요청을 처리하는 Service.
 *
 * 주요 흐름:
 * 1. 주문 조회
 * 2. 본인 주문 검증
 * 3. 결제 정보 조회
 * 4. 환불 가능 상태 검증
 * 5. 환불 상품별 금액 계산
 * 6. 환불 기록 저장
 * 7. 재고 복구
 * 8. 포인트 반환 / 적립 포인트 회수
 * 9. 결제 상태 변경
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
     * 환불 생성 메인 메서드.
     *
     * Controller에서 로그인 회원 ID, 주문 ID, 환불 요청 DTO를 받아 호출한다.
     */
    @Transactional
    public CreateRefundResponseDto createRefund(
        Long memberId,
        Long orderId,
        CreateRefundRequestDto requestDto
    ) {
        // 1. 주문 ID로 주문을 조회한다.
        Order order = findOrder(orderId);

        // 2. 로그인한 회원의 주문이 맞는지 검증한다.
        validateOrderOwner(order, memberId);

        // 3. 주문과 연결된 결제 정보를 조회한다.
        Payment payment = findPayment(order);

        // 4. 결제가 환불 가능한 상태인지 검증한다.
        validateRefundablePayment(payment);

        // 5. 환불 요청 상품별로 환불 가능 수량과 환불 금액을 계산한다.
        List<RefundItemCalculation> calculations = createRefundItemCalculations(
            payment,
            orderId,
            requestDto
        );

        // 6. 상품별 계산 결과를 바탕으로 총 포인트 환불 금액을 계산한다.
        Integer totalPointRefundAmount = calculateTotalPointRefundAmount(calculations);

        // 7. 상품별 계산 결과를 바탕으로 총 PG 환불 금액을 계산한다.
        Integer totalPgRefundAmount = calculateTotalPgRefundAmount(calculations);

        // 8. 포인트 환불 금액 + PG 환불 금액을 합쳐 총 환불 금액을 계산한다.
        Integer totalRefundAmount = calculateTotalRefundAmount(calculations);

        // 9. 이번 환불 후 결제 상태를 부분 환불 또는 전체 환불로 변경한다.
        changePaymentStatus(payment, totalRefundAmount);

        // 10. 환불 전체 기록을 저장한다.
        Refund refund = saveRefund(
            payment,
            requestDto.reason(),
            totalPointRefundAmount,
            totalPgRefundAmount
        );

        // 11. 환불 상품 상세 기록들을 저장한다.
        List<RefundItem> refundItems = saveRefundItems(refund, calculations);

        // 12. 환불된 상품 수량만큼 재고를 복구한다.
        restoreStock(calculations);

        // 13. 주문 회원 정보를 가져온다.
        Member member = order.getMember();

        // 14. 결제 때 사용했던 포인트 중 환불 대상 금액만큼 다시 돌려준다.
        refundUsedPoint(member, payment, totalPointRefundAmount);

        // 15. 결제 때 적립했던 포인트 중 환불 대상 금액만큼 회수한다.
        cancelEarnedPoint(member, payment, totalRefundAmount);

        // 16. 저장된 환불 정보를 응답 DTO로 변환해서 반환한다.
        return CreateRefundResponseDto.from(refund, refundItems);
    }

    /**
     * 주문 ID로 주문을 조회한다.
     * 주문이 없으면 ORDER_NOT_FOUND 예외를 발생시킨다.
     */
    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    /**
     * 주문 객체로 결제 정보를 조회한다.
     * 결제 정보가 없으면 PAYMENT_NOT_FOUND 예외를 발생시킨다.
     */
    private Payment findPayment(Order order) {
        return paymentRepository.findByOrder(order)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    /**
     * 로그인한 회원의 주문인지 검증한다.
     * 다른 회원의 주문이면 환불할 수 없다.
     */
    private void validateOrderOwner(Order order, Long memberId) {
        if (!order.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }
    }

    /**
     * 환불 가능한 결제 상태인지 검증한다.
     *
     * COMPLETED:
     * - 아직 환불되지 않은 결제 완료 상태
     *
     * PARTIAL_REFUNDED:
     * - 이미 일부 환불됐지만 남은 수량에 대해 추가 환불이 가능한 상태
     */
    private void validateRefundablePayment(Payment payment) {
        if (payment.getStatus() != PaymentStatus.COMPLETED
            && payment.getStatus() != PaymentStatus.PARTIAL_REFUNDED) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PAID);
        }
    }

    /**
     * 주문 상품 ID와 주문 ID로 주문 상품을 조회한다.
     *
     * orderItemId만으로 조회하지 않고 orderId도 같이 확인하는 이유:
     * - 다른 주문에 속한 주문 상품을 환불 요청하는 것을 막기 위해서다.
     */
    private OrderItem findOrderItem(Long orderItemId, Long orderId) {
        return orderItemRepository.findByIdAndOrder_Id(orderItemId, orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));
    }

    /**
     * 요청한 환불 수량이 실제 환불 가능한 수량 이하인지 검증한다.
     *
     * 환불 가능 수량 = 주문 수량 - 이미 환불된 수량
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
     * 환불 금액 = 주문 당시 상품 가격 * 환불 수량
     */
    private Integer calculateItemRefundAmount(OrderItem orderItem, Integer requestQuantity) {
        return orderItem.getPrice() * requestQuantity;
    }

    /**
     * 전체 환불 금액 중 포인트로 돌려줄 금액을 계산한다.
     *
     * 예:
     * 총 결제금액 10,000원
     * 사용 포인트 3,000원
     * 이번 환불금액 5,000원
     *
     * 포인트 환불금액 = 5,000 * 3,000 / 10,000 = 1,500원
     */
    private Integer calculatePointRefundAmount(Payment payment, Integer refundAmount) {
        if (payment.getTotalAmount() == 0) {
            return 0;
        }

        return refundAmount * payment.getUsePoint() / payment.getTotalAmount();
    }

    /**
     * 전체 환불 금액 중 PG로 환불할 금액을 계산한다.
     *
     * PG 환불금액 = 전체 환불금액 - 포인트 환불금액
     */
    private Integer calculatePgRefundAmount(Integer refundAmount, Integer pointRefundAmount) {
        return refundAmount - pointRefundAmount;
    }

    /**
     * 환불 금액 비율에 따라 회수할 적립 포인트를 계산한다.
     *
     * 예:
     * 총 결제금액 10,000원
     * 적립 포인트 100원
     * 이번 환불금액 5,000원
     *
     * 회수할 적립 포인트 = 5,000 * 100 / 10,000 = 50원
     */
    private Integer calculateCancelEarnPoint(Payment payment, Integer refundAmount) {
        if (payment.getTotalAmount() == 0) {
            return 0;
        }

        return refundAmount * payment.getEarnedPoint() / payment.getTotalAmount();
    }

    /**
     * 이번 환불 이후 결제 상태를 변경한다.
     *
     * 누적 환불금액 > 총 결제금액:
     * - 비정상 상황이므로 예외 발생
     *
     * 누적 환불금액 == 총 결제금액:
     * - 전체 환불 상태로 변경
     *
     * 누적 환불금액 < 총 결제금액:
     * - 부분 환불 상태로 변경
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
     *
     * 여기서 하는 일:
     * 1. 주문 상품 조회
     * 2. 환불 가능 수량 검증
     * 3. 상품별 총 환불 금액 계산
     * 4. 포인트 환불 금액 계산
     * 5. PG 환불 금액 계산
     * 6. 계산 결과를 RefundItemCalculation에 담기
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
     * 환불 상품 1개에 대한 계산 결과를 담는 Service 내부 전용 record.
     *
     * DB에 저장되는 Entity가 아니다.
     * RefundItem을 만들기 전에 임시로 계산 결과를 보관하는 용도다.
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
        Integer totalRefundAmount() {
            return pointRefundAmount + pgRefundAmount;
        }
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
     *
     * Refund:
     * - 환불 1건의 대표 정보
     * - 결제 정보, 환불 사유, 총 포인트 환불 금액, 총 PG 환불 금액을 가진다.
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
     *
     * RefundItem:
     * - 환불 1건 안에 포함된 상품별 상세 정보
     * - 어떤 주문 상품을 몇 개 환불했는지 기록한다.
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
     *
     * 예:
     * 주문 시 재고 -2
     * 환불 시 재고 +2
     */
    private void restoreStock(List<RefundItemCalculation> calculations) {
        for (RefundItemCalculation calculation : calculations) {
            calculation.orderItem().getProduct().increaseStock(calculation.quantity());
        }
    }

    /**
     * 결제 때 사용했던 포인트를 회원에게 다시 돌려준다.
     *
     * 포인트를 돌려준 뒤 PointTransaction 원장에 REFUND_USE 기록을 남긴다.
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
     *
     * 포인트를 회수한 뒤 PointTransaction 원장에 CANCEL_EARN 기록을 남긴다.
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
}
