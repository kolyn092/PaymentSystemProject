package com.paymentsystemproject.domain.order.dto;

import java.util.List;

public record GetOrderPreviewRequestDto(
    List<Long> cartItemIds
) {
}
