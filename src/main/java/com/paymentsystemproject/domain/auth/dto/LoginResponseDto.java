package com.paymentsystemproject.domain.auth.dto;

public record LoginResponseDto(
	String accessToken,
	String tokenType,
	long expiresIn
) {
	public static LoginResponseDto of(String accessToken, long expiresInMillis) {
		return new LoginResponseDto(
			accessToken,
			"Bearer",
			expiresInMillis / 1000
		);
	}
}
