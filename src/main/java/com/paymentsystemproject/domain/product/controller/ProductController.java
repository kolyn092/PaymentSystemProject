package com.paymentsystemproject.domain.product.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.domain.product.dto.GetOneProductResponseDto;
import com.paymentsystemproject.domain.product.dto.GetProductPageableResponseDto;
import com.paymentsystemproject.domain.product.service.ProductService;
import com.paymentsystemproject.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<GetProductPageableResponseDto>> list(
        @PageableDefault(page = 0, size = 10, sort = "price", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.ok(productService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GetOneProductResponseDto>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.findOneProduct(id)));
    }
}
