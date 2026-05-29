package com.paymentsystemproject.domain.cartitem.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentsystemproject.domain.cartitem.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
}
