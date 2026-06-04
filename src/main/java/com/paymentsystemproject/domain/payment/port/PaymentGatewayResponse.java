package com.paymentsystemproject.domain.payment.port;

public record PaymentGatewayResponse(
    String id,
    String status,
    int totalAmount
) {
}
