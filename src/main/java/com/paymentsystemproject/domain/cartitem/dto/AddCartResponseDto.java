package com.paymentsystemproject.domain.cartitem.dto;

import java.time.LocalDateTime;

import com.paymentsystemproject.domain.cartitem.entity.CartItem;

public record AddCartResponseDto(
    Long cartItemId,
    Long productId,
    int quantity,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AddCartResponseDto from(CartItem cartItem) {
        return new AddCartResponseDto(
            cartItem.getId(),
            cartItem.getProductId(),
            cartItem.getQuantity(),
            cartItem.getCreatedAt(),
            cartItem.getUpdatedAt()
        );
    }
}


