package com.paymentsystemproject.domain.product.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.product.dto.GetOneProductResponseDto;
import com.paymentsystemproject.domain.product.dto.GetProductListResponseDto;
import com.paymentsystemproject.domain.product.dto.GetProductPageableResponseDto;
import com.paymentsystemproject.domain.product.entity.Product;
import com.paymentsystemproject.domain.product.repository.ProductRepository;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public GetProductPageableResponseDto findAll(Pageable pageable) {
        Page<Product> productPage = productRepository.findAll(pageable);

        List<GetProductListResponseDto> content = productPage.getContent().stream()
            .map(this::toResponse)
            .toList();

        return new GetProductPageableResponseDto(
            content,
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalElements(),
            productPage.getTotalPages()
        );
    }

    public GetOneProductResponseDto findOneProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(
            () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
        );
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

    private GetProductListResponseDto toResponse(Product product) {
        return new GetProductListResponseDto(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getStock(),
            product.getStatus(),
            product.getCategory()
        );
    }

}
