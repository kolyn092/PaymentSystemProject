package com.paymentsystemproject.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentsystemproject.domain.order.entity.Orders;

public interface OrderRepository extends JpaRepository<Orders, Long> {
}
