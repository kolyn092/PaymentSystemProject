package com.paymentsystemproject.domain.order.dto;

import java.util.List;

public record GetOrderPreviewResponseDto(
    List<GetOrderPreviewItemDto> items,
    Integer totalAmount,
    Integer pointBalance
) {
    public static GetOrderPreviewResponseDto of(
        List<GetOrderPreviewItemDto> items,
        Integer totalAmount, Integer pointBalance) {
        return new GetOrderPreviewResponseDto(
            items,
            totalAmount,
            pointBalance);
    }
}
