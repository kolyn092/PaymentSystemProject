package com.paymentsystemproject.domain.product.dto;

import com.paymentsystemproject.domain.product.entity.Product;
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
    public static GetOneProductResponseDto from(Product product) {
        return new GetOneProductResponseDto(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getStock(),
            product.getDescription(),
            product.getStatus(),
            product.getCategory()
        );
    }
}
