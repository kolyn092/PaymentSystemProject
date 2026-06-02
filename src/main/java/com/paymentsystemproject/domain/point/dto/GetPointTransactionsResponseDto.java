package com.paymentsystemproject.domain.point.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;

import com.paymentsystemproject.domain.point.entity.PointTransaction;

public record GetPointTransactionsResponseDto(
    List<PointTransactionDetail> transactions,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static GetPointTransactionsResponseDto from(Page<PointTransaction> pointTransactions) {
        return new GetPointTransactionsResponseDto(
            pointTransactions.getContent().stream()
                .map(PointTransactionDetail::from)
                .toList(),
            pointTransactions.getNumber(),
            pointTransactions.getSize(),
            pointTransactions.getTotalElements(),
            pointTransactions.getTotalPages()
        );
    }

    public record PointTransactionDetail(
        Long pointTransactionId,
        String type,
        Integer amount,
        LocalDateTime createdAt
    ) {

        public static PointTransactionDetail from(PointTransaction pointTransaction) {
            return new PointTransactionDetail(
                pointTransaction.getId(),
                pointTransaction.getType(),
                pointTransaction.getAmount(),
                pointTransaction.getCreatedAt()
            );
        }
    }
}
