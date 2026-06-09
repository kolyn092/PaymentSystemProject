package com.paymentsystemproject.domain.cartitem.dto;

import java.util.List;

public record GetCartResponseDto(
    List<GetCartItemResponseDto> cartItems,
    int totalPrice
) {
}
