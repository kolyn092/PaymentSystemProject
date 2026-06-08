package com.paymentsystemproject.global.config;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.global.security.jwt.JwtAuthenticationEntryPoint;
import com.paymentsystemproject.global.security.jwt.JwtTokenProvider;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
	classes = {
		SecurityConfig.class,
		SecurityConfigTest.TestController.class,
		SecurityConfigTest.TestBeans.class
	},
	properties = "encoder.strength=4"
)
@AutoConfigureMockMvc
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("BCrypt PasswordEncoder를 생성한다")
	void passwordEncoder_success() {
		String encodedPassword = passwordEncoder.encode("password123");

		assertThat(encodedPassword).startsWith("$2");
		assertThat(passwordEncoder.matches("password123", encodedPassword)).isTrue();
		assertThat(passwordEncoder.matches("wrong-password", encodedPassword)).isFalse();
	}

	@Test
	@DisplayName("공개 엔드포인트는 토큰 없이 접근할 수 있다")
	void securityFilterChain_permitPublicEndpoint() throws Exception {
		mockMvc.perform(post("/api/signup"))
			.andExpect(status().isOk())
			.andExpect(content().string("signup-ok"));
	}

	@Test
	@DisplayName("보호된 엔드포인트는 인증이 필요하다")
	void securityFilterChain_rejectProtectedEndpointWithoutToken() throws Exception {
		mockMvc.perform(get("/api/protected"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("AUTH_001")));
	}

	@RestController
	static class TestController {

		@PostMapping("/api/signup")
		String signup() {
			return "signup-ok";
		}

		@GetMapping("/api/protected")
		String protectedApi() {
			return "protected-ok";
		}
	}

	@TestConfiguration
	static class TestBeans {

		@Bean
		JwtTokenProvider jwtTokenProvider() {
			return org.mockito.Mockito.mock(JwtTokenProvider.class);
		}

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(tools.jackson.databind.ObjectMapper objectMapper) {
			return new JwtAuthenticationEntryPoint(objectMapper);
		}
	}
}
