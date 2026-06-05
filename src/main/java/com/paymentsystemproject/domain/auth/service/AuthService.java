package com.paymentsystemproject.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.auth.dto.LoginRequestDto;
import com.paymentsystemproject.domain.auth.dto.LoginResponseDto;
import com.paymentsystemproject.domain.auth.dto.SignupRequestDto;
import com.paymentsystemproject.domain.auth.dto.SignupResponseDto;
import com.paymentsystemproject.domain.auth.repository.AuthRepository;
import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;
import com.paymentsystemproject.global.security.jwt.JwtTokenProvider;

@Service
public class AuthService {

	private final AuthRepository authRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthService(
		AuthRepository authRepository,
		PasswordEncoder passwordEncoder,
		JwtTokenProvider jwtTokenProvider
	) {
		this.authRepository = authRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Transactional
	public SignupResponseDto signup(SignupRequestDto request) {
		if (authRepository.existsByEmail(request.email())) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		Member member = new Member(
			request.email(),
			passwordEncoder.encode(request.password()),
			request.name(),
			request.phone()
		);

		Member saved = authRepository.save(member);

		return SignupResponseDto.from(saved);
	}

	@Transactional(readOnly = true)
	public LoginResponseDto login(LoginRequestDto request) {
		Member member = authRepository.findByEmail(request.email())
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(request.password(), member.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}

		String accessToken = jwtTokenProvider.createToken(member.getId(), member.getEmail());

		return LoginResponseDto.of(accessToken, jwtTokenProvider.getExpiration());
	}
}
