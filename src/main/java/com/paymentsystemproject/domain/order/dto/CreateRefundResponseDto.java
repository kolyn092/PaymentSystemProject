package com.paymentsystemproject.domain.order.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.paymentsystemproject.domain.order.entity.Refund;
import com.paymentsystemproject.domain.order.entity.RefundItem;

public record CreateRefundResponseDto(
    Long refundId,
    Long orderId,
    String refundStatus,
    Integer totalRefundAmount,
    Integer pointRefundAmount,
    Integer pgRefundAmount,
    String reason,
    List<RefundItemResponseDto> refundedItems,
    LocalDateTime createdAt
) {

    public static CreateRefundResponseDto from(Refund refund, List<RefundItem> refundItems) {
        return new CreateRefundResponseDto(
            refund.getId(),
            refund.getPayment().getOrder().getId(),
            refund.getStatus(),
            refund.getPointRefundAmount() + refund.getPgRefundAmount(),
            refund.getPointRefundAmount(),
            refund.getPgRefundAmount(),
            refund.getReason(),
            refundItems.stream()
                .map(RefundItemResponseDto::from)
                .toList(),
            refund.getCreatedAt()
        );
    }

    public record RefundItemResponseDto(
        Long orderItemId,
        String productName,
        Integer quantity,
        Integer refundAmount
    ) {

        public static RefundItemResponseDto from(RefundItem refundItem) {
            return new RefundItemResponseDto(
                refundItem.getOrderItem().getId(),
                refundItem.getOrderItem().getProductName(),
                refundItem.getQuantity(),
                refundItem.getPointRefundAmount() + refundItem.getPgRefundAmount()
            );
        }
    }
}
