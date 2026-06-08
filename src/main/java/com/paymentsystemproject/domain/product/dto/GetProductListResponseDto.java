package com.paymentsystemproject.domain.product.dto;

import java.time.LocalDateTime;

import com.paymentsystemproject.global.status.ProductCategory;
import com.paymentsystemproject.global.status.ProductStatus;

public record GetProductListResponseDto(
    Long id,
    String name,
    int price,
    int stock,
    ProductStatus status,
    ProductCategory category,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
