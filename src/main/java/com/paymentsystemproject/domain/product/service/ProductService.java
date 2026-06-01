package com.paymentsystemproject.domain.product.service;

import org.springframework.stereotype.Service;

import com.paymentsystemproject.domain.product.repository.ProductRepository;

@Service
public class ProductService {

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}
}
