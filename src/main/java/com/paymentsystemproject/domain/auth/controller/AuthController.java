package com.paymentsystemproject.domain.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.domain.auth.dto.LoginRequestDto;
import com.paymentsystemproject.domain.auth.dto.LoginResponseDto;
import com.paymentsystemproject.domain.auth.dto.SignupRequestDto;
import com.paymentsystemproject.domain.auth.dto.SignupResponseDto;
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
	public ResponseEntity<ApiResponse<SignupResponseDto>> signup(@Valid @RequestBody SignupRequestDto request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(authService.signup(request)));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.ok(authService.login(request)));
	}
}
