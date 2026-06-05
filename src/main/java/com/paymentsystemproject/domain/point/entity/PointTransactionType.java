package com.paymentsystemproject.domain.point.entity;

public enum PointTransactionType {

    EARN,
    USE,
    REFUND_USE,
    CANCEL_EARN;

    public Integer applySign(Integer amount) {
        return switch (this) {
            case EARN, REFUND_USE -> amount;
            case USE, CANCEL_EARN -> -amount;
        };
    }
}
