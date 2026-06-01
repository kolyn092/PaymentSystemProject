package com.paymentsystemproject.domain.cartitem.dto;

public record GetCartItemResponseDto(
    Long id,
    Long productId,
    String productName,
    int price,
    int quantity,
    int stock
) {
}

