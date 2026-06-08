package com.paymentsystemproject.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.paymentsystemproject.domain.order.entity.RefundItem;

public interface RefundItemRepository extends JpaRepository<RefundItem, Long> {

    @Query("""
        SELECT COALESCE(SUM(ri.quantity), 0)
        FROM RefundItem ri
        WHERE ri.orderItem.id = :orderItemId
        """)
    Integer sumRefundedQuantityByOrderItemId(@Param("orderItemId") Long orderItemId);
}
