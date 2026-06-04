package com.paymentsystemproject.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.paymentsystemproject.domain.order.entity.Refund;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    @Query("""
        SELECT COALESCE(SUM(r.pointRefundAmount + r.pgRefundAmount), 0)
        FROM Refund r
        WHERE r.payment.id = :paymentId
        """)
    Integer sumRefundAmountByPaymentId(@Param("paymentId") Long paymentId);
}
