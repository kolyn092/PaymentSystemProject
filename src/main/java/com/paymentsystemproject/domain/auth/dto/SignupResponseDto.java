package com.paymentsystemproject.domain.auth.dto;

import com.paymentsystemproject.domain.member.entity.Member;

public record SignupResponseDto(
	Long memberId,
	String email,
	String name
) {
	public static SignupResponseDto from(Member member) {
		return new SignupResponseDto(
			member.getId(),
			member.getEmail(),
			member.getName()
		);
	}
}
