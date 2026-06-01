package com.paymentsystemproject.domain.product.dto;

import java.util.List;

public record GetProductPageableResponseDto(
    List<GetProductListResponseDto> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPage
) {
}
