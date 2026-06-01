package com.paymentsystemproject.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentsystemproject.domain.member.entity.Member;

public interface AuthRepository extends JpaRepository<Member, Long> {
}
