package com.paymentsystemproject.domain.payment.entity;

// TODO - enum 값을 검증해줄 converter 작성 고려
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELED,
    PARTIAL_REFUNDED,
    REFUNDED
}
