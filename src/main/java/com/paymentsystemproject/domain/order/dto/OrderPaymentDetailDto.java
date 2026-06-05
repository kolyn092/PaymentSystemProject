package com.paymentsystemproject.domain.order.dto;

import java.time.LocalDateTime;

import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.domain.payment.entity.PaymentStatus;

public record OrderPaymentDetailDto(
    Long paymentId,
    String portonePaymentId,
    PaymentStatus status,
    LocalDateTime paidAt
) {
    public static OrderPaymentDetailDto from(Payment payment) {
        return new OrderPaymentDetailDto(
            payment.getId(),
            payment.getPortonePaymentId(),
            payment.getStatus(),
            payment.getPaidAt()
        );
    }
}
