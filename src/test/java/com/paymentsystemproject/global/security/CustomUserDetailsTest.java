package com.paymentsystemproject.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.paymentsystemproject.domain.member.entity.Member;

class CustomUserDetailsTest {

	@Test
	@DisplayName("회원 엔티티로 CustomUserDetails를 생성한다")
	void from_success() {
		Member member = new Member("user@example.com", "encoded-password", "User Name", "010-1234-5678");
		ReflectionTestUtils.setField(member, "id", 1L);

		CustomUserDetails userDetails = CustomUserDetails.from(member);

		assertThat(userDetails.getMemberId()).isEqualTo(1L);
		assertThat(userDetails.getUsername()).isEqualTo("user@example.com");
		assertThat(userDetails.getEmail()).isEqualTo("user@example.com");
		assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
		assertThat(userDetails.getAuthorities()).isEmpty();
	}

	@Test
	@DisplayName("토큰 정보로 비밀번호 없는 CustomUserDetails를 생성한다")
	void ofToken_success() {
		CustomUserDetails userDetails = CustomUserDetails.ofToken(1L, "user@example.com");

		assertThat(userDetails.getMemberId()).isEqualTo(1L);
		assertThat(userDetails.getUsername()).isEqualTo("user@example.com");
		assertThat(userDetails.getEmail()).isEqualTo("user@example.com");
		assertThat(userDetails.getPassword()).isNull();
		assertThat(userDetails.getAuthorities()).isEmpty();
	}
}
