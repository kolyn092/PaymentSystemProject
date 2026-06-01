package com.paymentsystemproject.domain.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentsystemproject.domain.payment.entity.WebhookEvent;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
}
