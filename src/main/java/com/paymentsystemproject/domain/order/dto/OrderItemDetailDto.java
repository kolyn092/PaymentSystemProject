package com.paymentsystemproject.domain.order.dto;

import com.paymentsystemproject.domain.order.entity.OrderItem;

public record OrderItemDetailDto(
    Long orderItemId,
    String productName,
    Integer price,
    Integer quantity,
    Integer subtotal
) {
    public static OrderItemDetailDto from(OrderItem orderItem) {
        return new OrderItemDetailDto(
            orderItem.getId(),
            orderItem.getProductName(),
            orderItem.getPrice(),
            orderItem.getQuantity(),
            orderItem.getPrice() * orderItem.getQuantity()
        );
    }
}
