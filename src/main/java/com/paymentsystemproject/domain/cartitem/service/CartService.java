package com.paymentsystemproject.domain.cartitem.service;

import org.springframework.stereotype.Service;

import com.paymentsystemproject.domain.cartitem.repository.CartRepository;

@Service
public class CartService {

	private final CartRepository cartRepository;

	public CartService(CartRepository cartRepository) {
		this.cartRepository = cartRepository;
	}
}
