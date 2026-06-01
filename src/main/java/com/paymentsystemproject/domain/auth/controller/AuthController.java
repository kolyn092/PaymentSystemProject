package com.paymentsystemproject.domain.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.domain.auth.dto.LoginRequest;
import com.paymentsystemproject.domain.auth.dto.LoginResponse;
import com.paymentsystemproject.domain.auth.dto.SignupRequest;
import com.paymentsystemproject.domain.auth.dto.SignupResponse;
import com.paymentsystemproject.domain.auth.service.AuthService;
import com.paymentsystemproject.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(authService.signup(request)));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.ok(authService.login(request)));
	}
}
