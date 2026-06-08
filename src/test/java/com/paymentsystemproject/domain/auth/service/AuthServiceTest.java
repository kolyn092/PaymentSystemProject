package com.paymentsystemproject.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.paymentsystemproject.domain.auth.dto.LoginRequestDto;
import com.paymentsystemproject.domain.auth.dto.LoginResponseDto;
import com.paymentsystemproject.domain.auth.dto.SignupRequestDto;
import com.paymentsystemproject.domain.auth.dto.SignupResponseDto;
import com.paymentsystemproject.domain.auth.repository.AuthRepository;
import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;
import com.paymentsystemproject.global.security.jwt.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AuthRepository authRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@InjectMocks
	private AuthService authService;

	@Test
	@DisplayName("회원가입 성공 시 비밀번호를 암호화하여 회원을 저장한다")
	void signup_success() {
		SignupRequestDto request = new SignupRequestDto(
			"user@example.com",
			"password123",
			"User Name",
			"010-1234-5678"
		);

		given(authRepository.existsByEmail(request.email())).willReturn(false);
		given(passwordEncoder.encode(request.password())).willReturn("encoded-password");
		given(authRepository.save(any(Member.class))).willAnswer(invocation -> {
			Member member = invocation.getArgument(0);
			ReflectionTestUtils.setField(member, "id", 1L);
			return member;
		});

		SignupResponseDto response = authService.signup(request);

		assertThat(response.memberId()).isEqualTo(1L);
		assertThat(response.email()).isEqualTo("user@example.com");
		assertThat(response.name()).isEqualTo("User Name");

		ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
		then(authRepository).should().save(memberCaptor.capture());
		Member savedMember = memberCaptor.getValue();
		assertThat(savedMember.getEmail()).isEqualTo("user@example.com");
		assertThat(savedMember.getPassword()).isEqualTo("encoded-password");
		assertThat(savedMember.getName()).isEqualTo("User Name");
		assertThat(savedMember.getPhone()).isEqualTo("010-1234-5678");
		assertThat(savedMember.getPointBalance()).isZero();
		then(passwordEncoder).should().encode("password123");
	}

	@Test
	@DisplayName("회원가입 시 이메일이 중복되면 예외가 발생한다")
	void signup_fail_duplicateEmail() {
		SignupRequestDto request = new SignupRequestDto(
			"user@example.com",
			"password123",
			"User Name",
			"010-1234-5678"
		);

		given(authRepository.existsByEmail(request.email())).willReturn(true);

		assertThatThrownBy(() -> authService.signup(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DUPLICATE_EMAIL);

		then(passwordEncoder).shouldHaveNoInteractions();
		then(authRepository).should(never()).save(any(Member.class));
	}

	@Test
	@DisplayName("로그인 성공 시 JWT 토큰을 생성하여 반환한다")
	void login_success() {
		LoginRequestDto request = new LoginRequestDto("user@example.com", "password123");
		Member member = member(1L, "user@example.com", "encoded-password", "User Name", "010-1234-5678");

		given(authRepository.findByEmail(request.email())).willReturn(Optional.of(member));
		given(passwordEncoder.matches(request.password(), member.getPassword())).willReturn(true);
		given(jwtTokenProvider.createToken(member.getId(), member.getEmail())).willReturn("access-token");
		given(jwtTokenProvider.getExpiration()).willReturn(3_600_000L);

		LoginResponseDto response = authService.login(request);

		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(3600L);
		then(jwtTokenProvider).should().createToken(1L, "user@example.com");
	}

	@Test
	@DisplayName("로그인 시 이메일로 회원을 찾을 수 없으면 예외가 발생한다")
	void login_fail_emailNotFound() {
		LoginRequestDto request = new LoginRequestDto("missing@example.com", "password123");

		given(authRepository.findByEmail(request.email())).willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_CREDENTIALS);

		then(passwordEncoder).shouldHaveNoInteractions();
		then(jwtTokenProvider).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("로그인 시 비밀번호가 일치하지 않으면 예외가 발생한다")
	void login_fail_passwordMismatch() {
		LoginRequestDto request = new LoginRequestDto("user@example.com", "wrong-password");
		Member member = member(1L, "user@example.com", "encoded-password", "User Name", "010-1234-5678");

		given(authRepository.findByEmail(request.email())).willReturn(Optional.of(member));
		given(passwordEncoder.matches(request.password(), member.getPassword())).willReturn(false);

		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_CREDENTIALS);

		then(jwtTokenProvider).shouldHaveNoInteractions();
	}

	private Member member(Long id, String email, String password, String name, String phone) {
		Member member = new Member(email, password, name, phone);
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}
}
