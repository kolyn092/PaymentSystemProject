package com.paymentsystemproject.domain.infra.portone.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

}
