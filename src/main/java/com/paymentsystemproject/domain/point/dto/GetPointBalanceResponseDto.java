package com.paymentsystemproject.domain.point.dto;

import com.paymentsystemproject.domain.member.entity.Member;

public record GetPointBalanceResponseDto(Integer balance) {

    public static GetPointBalanceResponseDto from(Member member) {
        return new GetPointBalanceResponseDto(member.getPointBalance());
    }
}
