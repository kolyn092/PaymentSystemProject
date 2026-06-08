package com.paymentsystemproject.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

class JwtAuthenticationEntryPointTest {

	@Test
	@DisplayName("인증 실패 시 401 API 응답을 작성한다")
	void commence_success() throws Exception {
		JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(new tools.jackson.databind.ObjectMapper());
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		entryPoint.commence(request, response, mock(AuthenticationException.class));

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentType()).contains("application/json");
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
		assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains("AUTH_001");
	}
}
