package com.paymentsystemproject.global.logging;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApiLoggingFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		long startedAt = System.currentTimeMillis();

		try {
			filterChain.doFilter(request, response);
		} finally {
			log.info(
				"[API-LOG] method={} uri={} status={} durationMs={}",
				request.getMethod(),
				request.getRequestURI(),
				response.getStatus(),
				System.currentTimeMillis() - startedAt
			);
		}
	}
}
