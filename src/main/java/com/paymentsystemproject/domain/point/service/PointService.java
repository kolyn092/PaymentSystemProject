package com.paymentsystemproject.domain.point.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.member.repository.MemberRepository;
import com.paymentsystemproject.domain.point.dto.GetPointBalanceResponseDto;
import com.paymentsystemproject.domain.point.dto.GetPointTransactionsResponseDto;
import com.paymentsystemproject.domain.point.entity.PointTransaction;
import com.paymentsystemproject.domain.point.repository.PointRepository;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final MemberRepository memberRepository;
    private final PointRepository pointRepository;

    public GetPointBalanceResponseDto getPointBalance(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return GetPointBalanceResponseDto.from(member);
    }

    public GetPointTransactionsResponseDto getPointTransactions(Long memberId, int page, int size) {
        validatePageRequest(page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<PointTransaction> pointTransactions = pointRepository.findAllByMember_IdOrderByCreatedAtDesc(
            memberId,
            pageable
        );

        return GetPointTransactionsResponseDto.from(pointTransactions);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0 || size < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
