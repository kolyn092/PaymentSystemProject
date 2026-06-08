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
    // 💡 엔티티를 바로 DTO로 변환해 주는 팩토리 메서드
    public static AddCartResponseDto from(CartItem cartItem) {
        return new AddCartResponseDto(
            cartItem.getId(),
            cartItem.getProductId(), // CartItem 엔티티 안에 있는 메서드 활용
            cartItem.getQuantity(),
            cartItem.getCreatedAt(),
            cartItem.getUpdatedAt()
        );
    }
}


