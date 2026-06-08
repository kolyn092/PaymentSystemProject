package com.paymentsystemproject.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

	private static final String SECRET = "test-secret-key-for-jwt-token-provider-1234567890";

	@Test
	@DisplayName("회원 ID와 이메일을 담은 유효한 토큰을 생성한다")
	void createToken_success() {
		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 3_600_000L);

		String token = jwtTokenProvider.createToken(1L, "user@example.com");

		assertThat(token).isNotBlank();
		assertThat(jwtTokenProvider.validateToken(token)).isTrue();
		assertThat(jwtTokenProvider.getMemberId(token)).isEqualTo(1L);
		assertThat(jwtTokenProvider.getEmail(token)).isEqualTo("user@example.com");
	}

	@Test
	@DisplayName("유효하지 않은 토큰이면 false를 반환한다")
	void validateToken_fail_invalidToken() {
		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 3_600_000L);

		boolean valid = jwtTokenProvider.validateToken("invalid-token");

		assertThat(valid).isFalse();
	}

	@Test
	@DisplayName("만료된 토큰이면 false를 반환한다")
	void validateToken_fail_expiredToken() {
		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, -60_000L);
		String token = jwtTokenProvider.createToken(1L, "user@example.com");

		boolean valid = jwtTokenProvider.validateToken(token);

		assertThat(valid).isFalse();
	}

	@Test
	@DisplayName("설정된 토큰 만료 시간을 반환한다")
	void getExpiration_success() {
		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 3_600_000L);

		assertThat(jwtTokenProvider.getExpiration()).isEqualTo(3_600_000L);
	}
}
