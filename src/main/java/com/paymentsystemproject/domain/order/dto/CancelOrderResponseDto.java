package com.paymentsystemproject.domain.order.dto;

import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.order.entity.OrderStatus;

public record CancelOrderResponseDto(
    Long orderId,
    OrderStatus status
) {
    public static CancelOrderResponseDto from(Order order) {
        return new CancelOrderResponseDto(
            order.getId(),
            order.getStatus()
        );
    }
}
