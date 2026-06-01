package com.paymentsystemproject.domain.order.entity;

import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refund")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "payment_id", nullable = false)
	private Payment payment;

	@Column(length = 500)
	private String reason;

	@Column(name = "point_refund_amount", nullable = false)
	private Integer pointRefundAmount;

	@Column(name = "pg_refund_amount", nullable = false)
	private Integer pgRefundAmount;

	@Column(length = 20, nullable = false)
	private String status;
}