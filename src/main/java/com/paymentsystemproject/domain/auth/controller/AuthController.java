package com.paymentsystemproject.domain.auth.controller;

import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.domain.auth.service.AuthService;

@RestController
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}
}
