package com.paymentsystemproject.domain.order.dto;

import com.paymentsystemproject.domain.cartitem.entity.CartItem;

public record GetOrderPreviewItemDto(
    Long productId,
    String productName,
    Integer price,
    Integer quantity,
    Integer subtotal
) {
    public static GetOrderPreviewItemDto from(CartItem cartItem) {
        return new GetOrderPreviewItemDto(
            cartItem.getProduct().getId(),
            cartItem.getProduct().getName(),
            cartItem.getProduct().getPrice(),
            cartItem.getQuantity(),
            cartItem.getProduct().getPrice() * cartItem.getQuantity()
        );
    }
}
