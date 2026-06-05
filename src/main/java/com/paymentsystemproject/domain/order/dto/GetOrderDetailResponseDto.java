package com.paymentsystemproject.domain.order.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.order.entity.OrderStatus;
import com.paymentsystemproject.domain.payment.entity.Payment;

public record GetOrderDetailResponseDto(
    Long orderId,
    String orderNumber,
    OrderStatus status,
    Integer totalAmount,
    Integer earnedPoint,
    List<OrderItemDetailDto> orderitems,
    OrderPaymentDetailDto payment,
    LocalDateTime createdAt
) {
    public static GetOrderDetailResponseDto from(Order order, Payment payment) {
        return new GetOrderDetailResponseDto(
            order.getId(),
            order.getOrderNumber(),
            order.getStatus(),
            order.getTotalAmount(),
            payment.getEarnedPoint(),
            order.getOrderItems().stream().map(OrderItemDetailDto::from).toList(),
            OrderPaymentDetailDto.from(payment),
            order.getCreatedAt()
        );
    }
}
