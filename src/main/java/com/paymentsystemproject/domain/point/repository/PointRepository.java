package com.paymentsystemproject.domain.point.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentsystemproject.domain.point.entity.PointTransaction;

public interface PointRepository extends JpaRepository<PointTransaction, Long> {

    Page<PointTransaction> findAllByMember_IdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
}
