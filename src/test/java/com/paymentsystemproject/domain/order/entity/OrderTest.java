package com.paymentsystemproject.domain.order.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.paymentsystemproject.global.error.BusinessException;

public class OrderTest {

    @Test
    @DisplayName("결제 대기 상태 주문은 직접 취소할 수 있다")
    void cancelPendingOrder_success() {
        // given
        Order order = createOrderWithStatus(OrderStatus.PENDING_PAYMENT);

        // when
        order.cancelPendingOrder();

        // then
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("결제 완료 상태 주문은 직접 취소할 수 없다")
    void cancelPendingOrder_fail_whenCompleted() {
        // given
        Order order = createOrderWithStatus(OrderStatus.COMPLETED);
        order.complete();

        // when & then
        assertThrows(BusinessException.class, order::cancelPendingOrder);
    }

    @Test
    @DisplayName("결제 완료 상태 주문은 전액 환불로 취소할 수 있다")
    void cancelByFullRefund_success() {
        // given
        Order order = createOrderWithStatus(OrderStatus.COMPLETED);
        order.complete();

        // when
        order.cancelByFullRefund();

        // then
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("결제 대기 상태 주문은 전액 환불로 취소할 수 없다")
    void cancelByFullRefund_fail_whenPendingPayment() {
        // given
        Order order = createOrderWithStatus(OrderStatus.PENDING_PAYMENT);

        // when & then
        assertThrows(BusinessException.class, order::cancelByFullRefund);
    }

    private Order createOrderWithStatus(OrderStatus status) {
        Order order = new Order();

        ReflectionTestUtils.setField(order, "status", status);
        ReflectionTestUtils.setField(order, "orderItems", new ArrayList<>());

        return order;
    }
}
