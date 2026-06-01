package com.paymentsystemproject.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentsystemproject.domain.order.entity.Refund;

public interface RefundRepository extends JpaRepository<Refund, Long> {
}
