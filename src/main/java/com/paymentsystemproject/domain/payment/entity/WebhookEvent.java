package com.paymentsystemproject.domain.payment.entity;

import java.time.LocalDateTime;

import com.paymentsystemproject.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "webhook_event",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_webhook_event_webhook_id",
		columnNames = "webhook_id"
	)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookEvent extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "webhook_id", length = 100, nullable = false)
	private String webhookId;

	@Column(name = "event_type", length = 50, nullable = false)
	private String eventType;

	@Column(length = 20, nullable = false)
	private String status;

	@Column(columnDefinition = "TEXT")
	private String payload;

	@Column(name = "fail_reason", length = 500)
	private String failReason;

	@Column(name = "processed_at")
	private LocalDateTime processedAt;
}
