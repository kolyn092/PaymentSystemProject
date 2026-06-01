package com.paymentsystemproject.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentsystemproject.domain.order.entity.RefundItem;

public interface RefundItemRepository extends JpaRepository<RefundItem, Long> {
}
