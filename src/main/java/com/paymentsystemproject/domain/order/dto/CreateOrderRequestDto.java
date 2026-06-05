package com.paymentsystemproject.domain.order.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateOrderRequestDto(
    @NotEmpty List<Long> cartItemIds,
    @PositiveOrZero Integer usePoint
) {
    public CreateOrderRequestDto {
        if (usePoint == null) {
            usePoint = 0;
        }
    }
}
