package com.paymentsystemproject.domain.order.service;

import org.springframework.stereotype.Service;

import com.paymentsystemproject.domain.order.repository.OrderRepository;

@Service
public class OrderService {

	private final OrderRepository orderRepository;

	public OrderService(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}
}
