package com.paymentsystemproject.domain.point.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.member.repository.MemberRepository;
import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.domain.point.dto.GetPointBalanceResponseDto;
import com.paymentsystemproject.domain.point.dto.GetPointTransactionsResponseDto;
import com.paymentsystemproject.domain.point.entity.PointTransaction;
import com.paymentsystemproject.domain.point.entity.PointTransactionType;
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

    @Transactional
    public void recordEarnPoint(
        Member member,
        Payment payment,
        Integer amount
    ) {
        recordPointTransaction(member, payment, PointTransactionType.EARN, amount);
    }

    @Transactional
    public void recordUsePoint(
        Member member,
        Payment payment,
        Integer amount
    ) {
        recordPointTransaction(member, payment, PointTransactionType.USE, amount);
    }

    @Transactional
    public void recordRefundUsePoint(
        Member member,
        Payment payment,
        Integer amount
    ) {
        recordPointTransaction(member, payment, PointTransactionType.REFUND_USE, amount);
    }

    @Transactional
    public void recordCancelEarnPoint(
        Member member,
        Payment payment,
        Integer amount
    ) {
        recordPointTransaction(member, payment, PointTransactionType.CANCEL_EARN, amount);
    }

    @Transactional
    public void refundUsedPoint(
        Member member,
        Payment payment,
        Integer amount
    ) {
        if (amount == null || amount <= 0) {
            return;
        }

        member.increasePoint(amount);

        recordPointTransaction(member, payment, PointTransactionType.REFUND_USE, amount);
    }

    @Transactional
    public void cancelEarnedPoint(
        Member member,
        Payment payment,
        Integer amount
    ) {
        if (amount == null || amount <= 0) {
            return;
        }

        member.decreasePoint(amount);

        recordPointTransaction(member, payment, PointTransactionType.CANCEL_EARN, amount);
    }

    private void recordPointTransaction(
        Member member,
        Payment payment,
        PointTransactionType type,
        Integer amount
    ) {
        if (amount == null || amount <= 0) {
            return;
        }

        PointTransaction pointTransaction = PointTransaction.create(
            member,
            payment,
            type,
            amount
        );

        pointRepository.save(pointTransaction);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0 || size < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
