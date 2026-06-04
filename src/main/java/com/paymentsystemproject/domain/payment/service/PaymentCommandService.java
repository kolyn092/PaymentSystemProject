package com.paymentsystemproject.domain.payment.service;

import org.springframework.stereotype.Service;

import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.payment.dto.PaymentConfirmResponseDto;
import com.paymentsystemproject.domain.payment.entity.Payment;
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

    // todo - 실패 케이스 처리

    /**
     * 결제 실패 처리
     *
     * 흐름:
     *   1) 주문 ID로 결제 + 주문을 함께 조회 (fetch join)
     *   2) 결제 상태 → FAILED
     *   3) 주문 상태 → CANCELED
     *   4) 주문에 포함된 상품들의 재고를 원래대로 복구
     *   5) 장바구니 유지
     *
     *   실패 케이스:
     *   1. 금액 불일치
     *   2. 결제 상태 실패
     *   3. 주문 소유자 불일치
     *   4. 이미 처리된 결제
     *   5. 존재하지 않는 주문
     *
     * 주문 생성 시 차감했던 재고를 되돌려 놓아야 다른 고객이 구매할 수 있으므로 재고 복구가 필수다.
     * - @Transactional 로 묶어 전체가 원자적으로 실행된다.
     */
    @Transactional
    public void failPaymentAndOrder(Long orderId) {
        // 결제 & 주문 조회
        Payment payment = paymentRepository.findByOrderIdWithOrderForUpdate(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        Order order = payment.getOrder();

        // 결제 상태를 실패로 변경
        payment.markAsFailed();

        // 주문 상태를 취소로 변경
        // orderService.cancelOrder(order);

        // 재고 원상 복구
        // restoreStock(order);
    }

    /**
     * 결제 승인 처리
     *
     * 흐름:
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
     *
     */
    @Transactional
    public PaymentConfirmResponseDto approvePaymentAndOrder(Long orderId) {
        // 1) 주문 & 결제 조회
        Payment payment = paymentRepository.findByOrderIdWithOrderForUpdate(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        Order order = payment.getOrder();

        // 2) 결제 상태 -> COMPLETED로 변경
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

        // dto 생성
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
