package com.paymentsystemproject.domain.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.payment.entity.Payment;

import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder(Order order);

    // 주문 단건 상세 조회 : orderId만으로 조회
    @Query("SELECT p FROM Payment p JOIN FETCH p.order WHERE p.order.id = :orderId")
    Optional<Payment> findByOrderIdWithOrder(@Param("orderId") Long orderId);

    //DB 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Payment p
            join fetch p.order o
            where o.id = :orderId
        """)
    Optional<Payment> findByOrderIdWithOrderForUpdate(@Param("orderId") Long orderId);

    // order과 member 소유권 같이 검증
    @Query("""
            select p
            from Payment p
            join fetch p.order o
            where o.id = :orderId
              and o.member.id = :memberId
        """)
    Optional<Payment> findByOrderIdAndMemberId(
        Long orderId,
        Long memberId
    );

}
