package com.paymentsystemproject.domain.order.dto;

import java.time.LocalDateTime;

import com.paymentsystemproject.domain.order.entity.Order;

public record GetOrderListResponseDto(
    Long orderId,
    String orderNumber,
    String status,
    Integer totalAmount,
    LocalDateTime createdAt
) {
    public static GetOrderListResponseDto from(Order order) {
        return new GetOrderListResponseDto(
            order.getId(),
            order.getOrderNumber(),
            order.getStatus().name(),
            order.getTotalAmount(),
            order.getCreatedAt()
        );
    }
}
