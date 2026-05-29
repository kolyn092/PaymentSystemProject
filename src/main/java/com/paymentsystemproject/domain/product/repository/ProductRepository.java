package com.paymentsystemproject.domain.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentsystemproject.domain.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
