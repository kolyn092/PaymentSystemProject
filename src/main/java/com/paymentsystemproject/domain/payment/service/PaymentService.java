package com.paymentsystemproject.domain.payment.service;

import org.springframework.stereotype.Service;

import com.paymentsystemproject.domain.payment.repository.PaymentRepository;

@Service
public class PaymentService {

	private final PaymentRepository paymentRepository;

	public PaymentService(PaymentRepository paymentRepository) {
		this.paymentRepository = paymentRepository;
	}
}
