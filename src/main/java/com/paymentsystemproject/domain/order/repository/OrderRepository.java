package com.paymentsystemproject.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentsystemproject.domain.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
