package com.paymentsystemproject.domain.cartitem.dto;

import com.paymentsystemproject.domain.cartitem.entity.CartItem;

public record GetCartItemResponseDto(
    Long id,
    Long productId,
    String productName,
    int price,
    int quantity,
    int stock
) {
    public static GetCartItemResponseDto from(CartItem item) {
        return new GetCartItemResponseDto(
            item.getId(),
            item.getProduct().getId(),
            item.getProduct().getName(),
            item.getProduct().getPrice(),
            item.getQuantity(),
            item.getProduct().getStock()
        );
    }
}

