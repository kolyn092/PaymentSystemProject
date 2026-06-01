package com.paymentsystemproject.domain.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentsystemproject.domain.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
