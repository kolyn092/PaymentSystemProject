package com.paymentsystemproject.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.paymentsystemproject.global.security.CustomUserDetails;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@BeforeEach
	void setUp() {
		jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider);
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("Bearer 토큰이 유효하면 인증 정보를 설정한다")
	void doFilter_success_validBearerToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		request.addHeader("Authorization", "Bearer access-token");

		given(jwtTokenProvider.validateToken("access-token")).willReturn(true);
		given(jwtTokenProvider.getMemberId("access-token")).willReturn(1L);
		given(jwtTokenProvider.getEmail("access-token")).willReturn("user@example.com");

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication).isNotNull();
		assertThat(authentication.isAuthenticated()).isTrue();
		assertThat(authentication.getPrincipal()).isInstanceOf(CustomUserDetails.class);

		CustomUserDetails principal = (CustomUserDetails)authentication.getPrincipal();
		assertThat(principal.getMemberId()).isEqualTo(1L);
		assertThat(principal.getUsername()).isEqualTo("user@example.com");
	}

	@Test
	@DisplayName("Authorization 헤더가 없으면 인증을 설정하지 않는다")
	void doFilter_skip_missingAuthorizationHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		then(jwtTokenProvider).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("토큰이 유효하지 않으면 인증을 설정하지 않는다")
	void doFilter_skip_invalidToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		request.addHeader("Authorization", "Bearer invalid-token");

		given(jwtTokenProvider.validateToken("invalid-token")).willReturn(false);

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		then(jwtTokenProvider).should(never()).getMemberId("invalid-token");
		then(jwtTokenProvider).should(never()).getEmail("invalid-token");
	}

	@Test
	@DisplayName("기존 인증 정보가 있으면 덮어쓰지 않는다")
	void doFilter_skip_existingAuthentication() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		request.addHeader("Authorization", "Bearer access-token");
		Authentication existingAuthentication = new UsernamePasswordAuthenticationToken("existing", null);
		SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

		given(jwtTokenProvider.validateToken("access-token")).willReturn(true);

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existingAuthentication);
		then(jwtTokenProvider).should(never()).getMemberId("access-token");
		then(jwtTokenProvider).should(never()).getEmail("access-token");
	}
}
