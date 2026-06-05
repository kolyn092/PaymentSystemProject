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
    Optional<Payment> findByOrderIdWithOrderForUpdate(Long orderId);

    // Webhook에서 받아온 portonePaymentId 조건으로 Payment 조회 시 연관된 Order를 fetch join 으로 함께 로딩
    @Query("SELECT p FROM Payment p JOIN FETCH p.order WHERE p.portonePaymentId = :portonePaymentId")
    Optional<Payment> findByPortonePaymentId(@Param("portonePaymentId") String portonePaymentId);
}
