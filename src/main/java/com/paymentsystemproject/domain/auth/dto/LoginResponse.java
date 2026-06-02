package com.paymentsystemproject.domain.auth.dto;

public record LoginResponse(
	String accessToken,
	String tokenType,
	long expiresIn
) {
	public static LoginResponse of(String accessToken, long expiresInMillis) {
		return new LoginResponse(
			accessToken,
			"Bearer",
			expiresInMillis / 1000
		);
	}
}
