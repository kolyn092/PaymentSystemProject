package com.paymentsystemproject.domain.auth.service;

import org.springframework.stereotype.Service;

import com.paymentsystemproject.domain.auth.repository.AuthRepository;

@Service
public class AuthService {

	private final AuthRepository authRepository;

	public AuthService(AuthRepository authRepository) {
		this.authRepository = authRepository;
	}
}
