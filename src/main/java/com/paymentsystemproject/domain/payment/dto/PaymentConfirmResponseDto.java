package com.paymentsystemproject.domain.payment.dto;

import com.paymentsystemproject.domain.payment.entity.Payment;

public record PaymentConfirmResponseDto(
    Long paymentId,
    Long orderId,
    int amount,
    String paymentStatus,
    String orderStatus,
    String message
) {
    public static PaymentConfirmResponseDto of(
        Payment payment,
        String message
    ) {
        return new PaymentConfirmResponseDto(
            payment.getId(),
            payment.getOrder().getId(),
            payment.getTotalAmount(),
            payment.getStatus().name(),
            payment.getOrder().getStatus(),
            message
        );
    }
}
