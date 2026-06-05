package com.paymentsystemproject.domain.order.entity;

public enum OrderStatus {

    PENDING_PAYMENT,
    COMPLETED,
    CANCELLED,
    PARTIAL_REFUNDED,
    REFUNDED
}
