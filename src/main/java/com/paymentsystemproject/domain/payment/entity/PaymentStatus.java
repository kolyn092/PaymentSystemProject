package com.paymentsystemproject.domain.payment.entity;

import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

public enum PaymentStatus {

    PENDING,
    COMPLETED,
    FAILED,
    // 결제 취소(사용자 취소 || 시스템 취소)
    CANCELED,
    PARTIAL_REFUNDED,
    REFUNDED;

    public boolean canTransitionTo(PaymentStatus nextStatus) {
        return switch (this) {
            case PENDING -> nextStatus == COMPLETED ||
                nextStatus == FAILED ||
                nextStatus == CANCELED;

            case COMPLETED -> nextStatus == CANCELED ||
                nextStatus == PARTIAL_REFUNDED ||
                nextStatus == REFUNDED;

            case PARTIAL_REFUNDED -> nextStatus == REFUNDED;

            case FAILED, CANCELED, REFUNDED -> false;
        };
    }

    public void validateTransition(PaymentStatus nextStatus) {
        if (!canTransitionTo(nextStatus)) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS);
        }
    }
}
