package com.paymentsystemproject.domain.order.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByMember(Member member, Pageable pageable);

    @Query("""
                    SELECT o FROM Order o
                    JOIN FETCH o.orderItems
                    WHERE o.id =:orderId AND o.member.id = :memberId
        """)
    Optional<Order> findByIdAndMemberIdWithOrderItems(
        @Param("orderId") Long orderId,
        @Param("memberId") Long memberId
    );
}
