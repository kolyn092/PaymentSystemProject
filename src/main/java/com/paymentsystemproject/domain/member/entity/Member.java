package com.paymentsystemproject.domain.member.entity;

import com.paymentsystemproject.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "member",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_member_email",
		columnNames = "email"
	)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 255, nullable = false)
	private String email;

	@Column(length = 255, nullable = false)
	private String password;

	@Column(length = 50, nullable = false)
	private String name;

	@Column(length = 20)
	private String phone;

	@Column(name = "point_balance", nullable = false)
	private Integer pointBalance;

	public Member(String email, String password, String name, String phone) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.phone = phone;
		this.pointBalance = 0;
	}
}
