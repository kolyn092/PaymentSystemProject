package com.paymentsystemproject.domain.payment.controller;

import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.domain.payment.service.PaymentService;

@RestController
public class PaymentController {

	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}
}
