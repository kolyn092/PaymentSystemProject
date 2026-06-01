package com.paymentsystemproject.domain.point.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentsystemproject.domain.point.entity.PointTransaction;

public interface PointRepository extends JpaRepository<PointTransaction, Long> {
}
