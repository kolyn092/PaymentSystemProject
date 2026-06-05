package com.paymentsystemproject.domain.order.dto;

import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.payment.entity.Payment;

public record CreateOrderResponseDto(
    Long orderId,
    String orderNumber,
    String portonePaymentId,
    Integer totalAmount,
    Integer usePoint,
    Integer pgAmount
) {
    public static CreateOrderResponseDto from(Order order, Payment payment) {
        return new CreateOrderResponseDto(
            order.getId(),
            order.getOrderNumber(),
            payment.getPortonePaymentId(),
            order.getTotalAmount(),
            payment.getUsePoint(),
            payment.getPgAmount()
        );
    }
}
