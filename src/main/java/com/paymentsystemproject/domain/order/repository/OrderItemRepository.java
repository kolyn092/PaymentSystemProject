package com.paymentsystemproject.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentsystemproject.domain.order.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
