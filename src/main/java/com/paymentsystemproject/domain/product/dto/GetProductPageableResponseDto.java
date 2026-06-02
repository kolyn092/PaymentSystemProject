package com.paymentsystemproject.domain.product.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.paymentsystemproject.domain.product.entity.Product;

public record GetProductPageableResponseDto(
    List<GetProductListResponseDto> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPage
) {
    public static GetProductPageableResponseDto from(Page<Product> productPage,
        List<GetProductListResponseDto> content) {
        return new GetProductPageableResponseDto(
            content,
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalElements(),
            productPage.getTotalPages()
        );
    }
}
