package com.paymentsystemproject.domain.payment.facade;

import org.springframework.stereotype.Component;

import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.order.entity.OrderStatus;
import com.paymentsystemproject.domain.payment.dto.PaymentConfirmRequestDto;
import com.paymentsystemproject.domain.payment.dto.PaymentConfirmResponseDto;
import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.domain.payment.port.PaymentGateway;
import com.paymentsystemproject.domain.payment.port.PaymentGatewayResponseDto;
import com.paymentsystemproject.domain.payment.service.PaymentCommandService;
import com.paymentsystemproject.domain.payment.service.PaymentService;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
// 결제 프로세스의 흐름을 제어
public class PaymentFacade {

    private static final String PG_STATUS_COMPLETED = "COMPLETED";

    private final PaymentService paymentService;
    private final PaymentCommandService paymentCommandService;
    private final PaymentGateway paymentGateway;

    /**
     * 결제 승인 메인 흐름
     *
     *      1. Payment 조회, 주문 소유권 검증
     *      2. Order 조회
     *      3. portOnePaymentId 일치 검증
     *      4. 멱등성 검증
     *          - 이미 COMPLETED면 성공 응답 반환 또는 예외 처리
     *          - FAILED/CANCELED면 재승인 불가 예외
     *      5. Order 상태 검증
     *          - 결제 대기 상태인지 확인
     *      6. Payment 상태 검증
     *          - PENDING 상태인지 검증
     *      7. PortOne API로 실제 결제 조회
     *      8. PortOne 결제 상태 검증
     *          - COMPLETED 상태인지 확인
     *      9. 승인 금액 검증
     *          - PortOne 결제 금액 == payment.pgAmount
     *      10. DB 결제/주문 상태 승인 처리
     */
    public PaymentConfirmResponseDto confirmPayment(Long memberId, PaymentConfirmRequestDto request) {
        // 1, 3. Payment 조회 / 소유권 검증
        Payment payment = paymentService.findByOrderIdAndMemberId(request.orderId(), memberId);

        // 2. Order 조회
        Order order = payment.getOrder();

        // 3 ~ 6. 유효성 검증
        PaymentConfirmResponseDto response = validateConfirmRequest(request, payment, order);
        if (response != null) {
            return response;
        }

        // 전액 포인트 결제 인지 확인
        if (payment.isPointOnlyPayment()) {
            return paymentCommandService.confirmPointOnlyPayment(payment);
        }

        // 7. PortOne API로 실제 결제 조회
        PaymentGatewayResponseDto pgPayment = paymentGateway.getPayment(payment.getPortonePaymentId());

        // 8. PortOne 결제 상태 검증
        validatePgPaymentCompleted(payment, order, pgPayment);

        // 9. 승인 금액 검증 - PortOne 결제 금액 == payment.pgAmount
        validatePaymentAmount(payment, order, pgPayment);

        return paymentCommandService.completePayment(order.getId());
    }

    public void cancelPayment(Long memberId, Long orderId) {
        Payment payment = paymentService.findByOrderIdAndMemberId(orderId, memberId);

        if (!payment.isPointOnlyPayment()) {
            paymentGateway.cancelPayment(payment.getPortonePaymentId(), "사용자 결제 취소", null);
        }
        
        paymentCommandService.cancelPayment(payment.getOrder().getId());
    }

    // 기본 유효성 검증
    private PaymentConfirmResponseDto validateConfirmRequest(PaymentConfirmRequestDto request, Payment payment,
        Order order) {

        // 4. portOnePaymentId 일치 검증
        validatePortOnePaymentId(request, payment);

        // 5. 멱등성 검증
        PaymentConfirmResponseDto idempotencyResponse = handleIdempotency(payment);
        if (idempotencyResponse != null) {
            return idempotencyResponse;
        }

        // 6. Order 상태 검증
        validateOrderStatus(order);

        return null;
    }

    private void validatePortOnePaymentId(PaymentConfirmRequestDto request, Payment payment) {
        String portOnePaymentId = payment.getPortonePaymentId();

        if (!portOnePaymentId.equals(request.portonePaymentId())) {
            log.warn("결제 승인 거부 - portonePaymentId 불일치: paymentId={}, DB={}, 요청={}", payment.getId(), portOnePaymentId,
                request.portonePaymentId());

            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
    }

    private PaymentConfirmResponseDto handleIdempotency(Payment payment) {
        return switch (payment.getStatus()) {
            case COMPLETED -> PaymentConfirmResponseDto.of(payment, "이미 승인된 결제입니다.");
            case FAILED -> throw new BusinessException(ErrorCode.PAYMENT_ALREADY_FAILED);
            case CANCELED -> throw new BusinessException(ErrorCode.PAYMENT_ALREADY_CANCELED);
            case PENDING -> null;
            default -> throw new BusinessException(ErrorCode.ALREADY_PROCESSED_PAYMENT);
        };
    }

    // 주문 상태 검증
    private void validateOrderStatus(Order order) {
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }

    // PG 결제 상태가 완료인지 확인
    private void validatePgPaymentCompleted(Payment payment, Order order, PaymentGatewayResponseDto pgPayment) {
        if (!PG_STATUS_COMPLETED.equals(pgPayment.status())) {
            log.error("결제 승인 실패 - PG 상태 비정상: paymentId={}, pgStatus={}", payment.getId(), pgPayment.status());
            paymentCommandService.failPayment(order.getId());
            throw new BusinessException(ErrorCode.PAYMENT_NOT_PAID);
        }
    }

    // 서버 결제 금액과 PG 실제 결제 금액 비교
    private void validatePaymentAmount(Payment payment, Order order, PaymentGatewayResponseDto pgPayment) {
        if (payment.getPgAmount().equals(pgPayment.totalAmount())) {
            return;
        }

        log.error("결제 승인 실패 - 금액 불일치: paymentId={}, DB금액={}, PG금액={}", payment.getId(), payment.getPgAmount(),
            pgPayment.totalAmount());

        cancelPgPaymentSafely(payment.getPortonePaymentId());

        paymentCommandService.failPayment(order.getId());

        throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    // PG 결제 취소
    private void cancelPgPaymentSafely(String portonePaymentId) {
        try {
            paymentGateway.cancelPayment(portonePaymentId, "결제 금액 불일치 자동 취소", null);
        } catch (Exception e) {
            log.error("PG 자동 취소 실패 - 수동 처리 필요: portOnePaymentId={}", portonePaymentId, e);
        }
    }

}
