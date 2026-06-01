package com.paymentsystemproject.domain.product.dto;

import com.paymentsystemproject.global.status.ProductCategory;
import com.paymentsystemproject.global.status.ProductStatus;

public record GetOneProductResponseDto(
    Long id,
    String name,
    int price,
    int stock,
    String description,
    ProductStatus status,
    ProductCategory category
) {
}
