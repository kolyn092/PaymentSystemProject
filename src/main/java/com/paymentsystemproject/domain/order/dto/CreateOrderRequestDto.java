package com.paymentsystemproject.domain.order.dto;

import java.util.List;

public record CreateOrderRequestDto(
    List<Long> cartItemIds,
    Integer usePoint
) {
    public CreateOrderRequestDto {
        if (usePoint == null) {
            usePoint = 0;
        }
    }
}
