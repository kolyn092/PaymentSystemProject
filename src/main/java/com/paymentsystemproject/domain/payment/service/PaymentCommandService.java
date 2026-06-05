package com.paymentsystemproject.domain.payment.service;

import org.springframework.stereotype.Service;

import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.payment.dto.PaymentConfirmResponseDto;
import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.domain.payment.entity.PaymentStatus;
import com.paymentsystemproject.domain.payment.repository.PaymentRepository;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {

    private final PaymentRepository paymentRepository;
    // private final OrderService orderService;
    // private final ProductService productService;

    /**
     *   1) 주문 ID로 결제 + 주문을 함께 조회 (fetch join)
     *   2) 결제 상태 → FAILED
     *   3) 주문 상태 → CANCELED
     *   4) 주문에 포함된 상품들의 재고를 원래대로 복구
     *   5) 장바구니 유지
     */
    @Transactional
    public void failPayment(Long orderId) {
        // 결제 & 주문 조회
        Payment payment = paymentRepository.findByOrderIdWithOrderForUpdate(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        Order order = payment.getOrder();

        if (payment.getStatus() == PaymentStatus.FAILED) {
            return;
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED_PAYMENT);
        }

        // 결제 상태를 실패로 변경
        payment.markAsFailed();

        // 주문 상태를 취소로 변경
        // orderService.cancelOrder(order);

        // 재고 원상 복구
        // restoreStock(order);
    }

    @Transactional
    public void cancelPayment(Long orderId) {
        Payment payment = paymentRepository.findByOrderIdWithOrderForUpdate(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.CANCELED) {
            return;
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED_PAYMENT);
        }

        payment.markAsCanceled();

        // TODO: 주문 상태 CANCELED 변경
        // orderService.cancelOrder(payment.getOrder());

        // TODO: 환불/포인트 환급/적립 포인트 회수는 환불 담당 영역
    }

    /**
     *   1) 주문 ID로 결제 + 주문을 함께 조회 (fetch join)
     *   2) 결제 상태 → COMPLETED
     *   3) 주문 상태 → CONFIRMED
     *   4) 사용 포인트 차감
     *   5) 포인트 사용 기록 생성
     *   5) 포인트 적립
     *   6) 포인트 적립 기록 생성
     *   7) 포인트 잔액 갱신
     *   8) 장바구니 초기화
     *   9) 화면/클라이언트에 내려줄 응답 DTO 생성
     */
    @Transactional
    public PaymentConfirmResponseDto completePayment(Long orderId) {
        // 1) 주문 & 결제 조회
        Payment payment = paymentRepository.findByOrderIdWithOrderForUpdate(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        Order order = payment.getOrder();

        // 2) 결제 상태 -> COMPLETED로 변경
        // 멱등성 검증
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return PaymentConfirmResponseDto.of(payment, "이미 승인이 완료된 결제입니다.");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED_PAYMENT);
        }
        payment.markAsCompleted();

        // 3) 주문 상태 -> CONFIRMED로 변경
        // orderService.confirmOrder(order);

        // 4) 사용 포인트 차감
        // payment.getOrder().getMember().getPointBalance() -> ??;

        // 5) 포인트 사용 기록 생성

        // 5) 포인트 적립

        // 6) 포인트 적립 기록 생성

        // 7) 포인트 잔액 갱신

        // 8) 장바구니 초기화

        // 9) dto 생성
        return PaymentConfirmResponseDto.of(payment, "결제가 완료되었습니다.");
    }

    /**
     * 주문에 담긴 상품들의 재고를 복구한다.
     *
     * 주문이 생성될 때 상품 재고가 수량만큼 차감되므로,
     * 결제가 실패해서 주문이 취소되면 차감된 만큼 다시 더해 줘야 한다.
     * OrderItem은 상품의 "스냅샷"이므로 실제 재고 변경은 Product 엔티티에서 수행한다.
     */

    // 재고 원복 메서드
    // private void restoreStock(Order order) {
    //     for (OrderItem item : order.getOrderItems()) {
    //         Product product = productService.findProductEntity(item.getProductId());
    //         product.restoreStock(item.getQuantity());
    //     }
    // }

    // 포인트 차감 메서드

    // 포인트 적립 메서드

}
