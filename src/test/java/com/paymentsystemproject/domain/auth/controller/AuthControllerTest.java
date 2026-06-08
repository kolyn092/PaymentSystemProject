package com.paymentsystemproject.domain.auth.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.paymentsystemproject.domain.auth.dto.LoginRequestDto;
import com.paymentsystemproject.domain.auth.dto.LoginResponseDto;
import com.paymentsystemproject.domain.auth.dto.SignupRequestDto;
import com.paymentsystemproject.domain.auth.dto.SignupResponseDto;
import com.paymentsystemproject.domain.auth.service.AuthService;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;
import com.paymentsystemproject.global.error.GlobalExceptionHandler;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@Mock
	private AuthService authService;

	@BeforeEach
	void setUp() {
		AuthController authController = new AuthController(authService);

		mockMvc = MockMvcBuilders.standaloneSetup(authController)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();

		objectMapper = new ObjectMapper();
	}

	@Test
	@DisplayName("회원가입 요청 시 서비스 호출 후 201 응답을 반환한다")
	void signup_success() throws Exception {
		SignupRequestDto request = new SignupRequestDto(
			"user@example.com",
			"password123",
			"User Name",
			"010-1234-5678"
		);
		SignupResponseDto response = new SignupResponseDto(1L, "user@example.com", "User Name");

		given(authService.signup(request)).willReturn(response);

		mockMvc.perform(post("/api/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.data.memberId").value(1L))
			.andExpect(jsonPath("$.data.email").value("user@example.com"))
			.andExpect(jsonPath("$.data.name").value("User Name"));

		then(authService).should().signup(request);
	}

	@Test
	@DisplayName("회원가입 시 이메일이 중복되면 409 응답을 반환한다")
	void signup_fail_duplicateEmail() throws Exception {
		SignupRequestDto request = new SignupRequestDto(
			"user@example.com",
			"password123",
			"User Name",
			"010-1234-5678"
		);

		given(authService.signup(request)).willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL));

		mockMvc.perform(post("/api/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("AUTH_003"))
			.andExpect(jsonPath("$.message", notNullValue()));

		then(authService).should().signup(request);
	}

	@Test
	@DisplayName("회원가입 요청 값이 유효하지 않으면 400 응답을 반환한다")
	void signup_fail_invalidRequest() throws Exception {
		SignupRequestDto request = new SignupRequestDto(
			"invalid-email",
			"short",
			"",
			"01012345678"
		);

		mockMvc.perform(post("/api/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COMMON_001"))
			.andExpect(jsonPath("$.message", notNullValue()));

		then(authService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("로그인 요청 시 서비스 호출 후 200 응답을 반환한다")
	void login_success() throws Exception {
		LoginRequestDto request = new LoginRequestDto("user@example.com", "password123");
		LoginResponseDto response = new LoginResponseDto("access-token", "Bearer", 3600L);

		given(authService.login(request)).willReturn(response);

		mockMvc.perform(post("/api/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.expiresIn").value(3600L));

		then(authService).should().login(request);
	}

	@Test
	@DisplayName("로그인 인증 정보가 올바르지 않으면 401 응답을 반환한다")
	void login_fail_invalidCredentials() throws Exception {
		LoginRequestDto request = new LoginRequestDto("user@example.com", "wrong-password");

		given(authService.login(request)).willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

		mockMvc.perform(post("/api/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_004"))
			.andExpect(jsonPath("$.message", notNullValue()));

		then(authService).should().login(request);
	}

	@Test
	@DisplayName("로그인 요청 값이 유효하지 않으면 400 응답을 반환한다")
	void login_fail_invalidRequest() throws Exception {
		LoginRequestDto request = new LoginRequestDto("invalid-email", "");

		mockMvc.perform(post("/api/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COMMON_001"))
			.andExpect(jsonPath("$.message", notNullValue()));

		then(authService).shouldHaveNoInteractions();
	}
}
