package com.paymentsystemproject.domain.cartitem.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentsystemproject.domain.cartitem.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
