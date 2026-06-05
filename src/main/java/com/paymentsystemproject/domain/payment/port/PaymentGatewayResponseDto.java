package com.paymentsystemproject.domain.payment.port;

public record PaymentGatewayResponseDto(
    String id,
    String status,
    int totalAmount
) {
}
