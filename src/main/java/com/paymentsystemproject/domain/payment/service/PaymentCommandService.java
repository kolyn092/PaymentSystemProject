package com.paymentsystemproject.domain.payment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.cartitem.service.CartService;
import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.payment.dto.PaymentConfirmResponseDto;
import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.domain.payment.entity.PaymentStatus;
import com.paymentsystemproject.domain.payment.repository.PaymentRepository;
import com.paymentsystemproject.domain.point.service.PointService;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {

    private final PaymentRepository paymentRepository;
    private final CartService cartService;
    private final PointService pointService;

    /**
     *   1) 주문 ID로 결제 + 주문을 함께 조회 (fetch join)
     *   2) 결제 상태 → FAILED
     *   3) 주문 상태 → CANCELED
     *   4) 주문에 포함된 상품들의 재고를 원래대로 복구
     *   5) 장바구니 유지
     *
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
        order.cancel();
    }

    @Transactional
    public void cancelPayment(Long orderId) {
        Payment payment = paymentRepository.findByOrderIdWithOrderForUpdate(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        Order order = payment.getOrder();

        if (payment.getStatus() == PaymentStatus.CANCELED) {
            return;
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED_PAYMENT);
        }

        payment.markAsCanceled();
        order.cancel();
    }

    /**  성공 + 금액 일치
     *
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

        // 3) 주문 상태 -> COMPLETED로 변경
        order.complete();

        // 4) 사용 포인트 차감
        order.getMember().decreasePoint(payment.getUsePoint());

        // 5) 포인트 사용 기록 생성
        pointService.recordUsePoint(order.getMember(), payment, payment.getUsePoint());

        // 5) 포인트 적립
        int earnedPoint = earnedPoint(payment.getPgAmount());
        order.getMember().increasePoint(earnedPoint);

        // 6) 포인트 적립 기록 생성
        pointService.recordEarnPoint(order.getMember(), payment, earnedPoint);

        // 결제 상태 -> COMPLETED로 변경
        payment.markAsCompleted(earnedPoint);

        // 8) 장바구니 초기화
        cartService.removeCart(order.getMember().getId());

        // 9) dto 생성
        return PaymentConfirmResponseDto.of(payment, "결제가 완료되었습니다.");
    }

    // Order 상태 COMPLETED
    // Payment 상태 COMPLETED
    // 포인트 차감
    // 포인트 사용 원장 생성
    // 장바구니 초기화
    // 적립 포인트는 없음
    @Transactional
    public PaymentConfirmResponseDto confirmPointOnlyPayment(Payment payment) {
        // order 상태 completed
        payment.getOrder().complete();

        // payment 상태 completed
        payment.markAsCompleted(0);

        // 포인트 차감
        payment.getOrder().getMember().decreasePoint(payment.getUsePoint());

        // 포인트 사용 원장 생성
        pointService.recordUsePoint(payment.getOrder().getMember(), payment, payment.getUsePoint());

        // 장바구니 초기화
        cartService.removeCart(payment.getOrder().getMember().getId());

        return PaymentConfirmResponseDto.of(payment, "포인트 전액 결제가 완료되었습니다.");
    }

    // 포인트 계산 메서드
    private int earnedPoint(int pgAmount) {
        if (pgAmount == 0) {
            return 0;
        }

        return pgAmount / 100;
    }

}
