package com.paymentsystemproject.domain.cartitem.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.domain.cartitem.service.CartService;

@RestController
@RequestMapping("/api/cartitems")
public class CartController {

	private final CartService cartService;

	public CartController(CartService cartService) {
		this.cartService = cartService;
	}
}
