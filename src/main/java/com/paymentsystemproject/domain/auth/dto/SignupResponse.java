package com.paymentsystemproject.domain.auth.dto;

import com.paymentsystemproject.domain.member.entity.Member;

public record SignupResponse(
	Long memberId,
	String email,
	String name
) {
	public static SignupResponse from(Member member) {
		return new SignupResponse(
			member.getId(),
			member.getEmail(),
			member.getName()
		);
	}
}
