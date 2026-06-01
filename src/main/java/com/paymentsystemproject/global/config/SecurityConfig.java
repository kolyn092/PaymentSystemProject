package com.paymentsystemproject.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.paymentsystemproject.global.security.jwt.JwtAuthenticationEntryPoint;
import com.paymentsystemproject.global.security.jwt.JwtAuthenticationFilter;
import com.paymentsystemproject.global.security.jwt.JwtTokenProvider;

@Configuration
public class SecurityConfig {

	private static final String[] PUBLIC_ENDPOINTS = {
		"/signup",
		"/login"
	};

	private final JwtTokenProvider jwtTokenProvider;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Value("${encoder.strength}")
	private int strength;

	public SecurityConfig(JwtTokenProvider jwtTokenProvider,
		JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(FormLoginConfigurer<HttpSecurity>::disable)
			.httpBasic(HttpBasicConfigurer<HttpSecurity>::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(
				session ->
					session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(
				handler ->
					handler.authenticationEntryPoint(jwtAuthenticationEntryPoint))
			.authorizeHttpRequests(
				auth ->
					auth
						.requestMatchers(PUBLIC_ENDPOINTS)
						.permitAll()
						.anyRequest()
						.authenticated()
			)
			.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(strength);
	}
}
