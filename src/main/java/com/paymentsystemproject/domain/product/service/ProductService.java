package com.paymentsystemproject.domain.product.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.order.repository.OrderItemRepository;
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
    private final OrderItemRepository orderItemRepository;

    public GetProductPageableResponseDto findAll(
        String category, Integer minPrice, Integer maxPrice, String status, Pageable pageable) {

        Specification<Product> spec = Specification.where((root, query, cb) -> cb.isTrue(cb.literal(true)));

        if (category != null && !category.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        if (minPrice != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }
        if (status != null && !status.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<GetProductListResponseDto> content = productPage.getContent().stream()
            .map(this::toResponse)
            .toList();

        return GetProductPageableResponseDto.from(productPage, content);
    }

    public GetOneProductResponseDto findOneProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(
            () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
        );
        return GetOneProductResponseDto.from(product);
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
