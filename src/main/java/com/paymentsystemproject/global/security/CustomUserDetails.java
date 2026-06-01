package com.paymentsystemproject.global.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.paymentsystemproject.domain.member.entity.Member;

import lombok.Getter;

@Getter
public class CustomUserDetails implements UserDetails {

	private final Long memberId;
	private final String email;
	private final String password;

	private CustomUserDetails(Long memberId, String email, String password) {
		this.memberId = memberId;
		this.email = email;
		this.password = password;
	}

	public static CustomUserDetails from(Member member) {
		return new CustomUserDetails(member.getId(), member.getEmail(), member.getPassword());
	}

	public static CustomUserDetails ofToken(Long memberId, String email) {
		return new CustomUserDetails(memberId, email, null);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.emptyList();
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}
}
