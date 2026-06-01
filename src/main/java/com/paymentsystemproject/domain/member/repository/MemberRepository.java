package com.paymentsystemproject.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentsystemproject.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
