package com.paymentsystemproject.domain.payment.port;

public interface PaymentGateway {

    // PG사에서 실제 결제 정보 조회 (금액 검증용)
    PaymentGatewayResponseDto getPayment(String paymentId);

    // 결제 전액 & 부분 취소
    void cancelPayment(String paymentId, String reason, Integer amount);

}
