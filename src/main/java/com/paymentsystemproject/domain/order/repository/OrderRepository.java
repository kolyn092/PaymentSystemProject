package com.paymentsystemproject.domain.order.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.order.entity.Order;

/**
 * 주문 레포지토리입니다.
 */

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 회원의 주문 목록을 페이징하여 조회합니다.
     */

    Page<Order> findByMember(Member member, Pageable pageable);

    /**
     * 주문 ID와 회원 ID로 주문과 주문 항목을 함께 조회합니다.
     * JOIN FETCH로 주문 항목을 한 번에 가져와 N+1 문제를 방지합니다.
     * 본인 주문이 아닌 경우 빈 Optional을 반환합니다.
     */

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
