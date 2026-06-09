package com.paymentsystemproject.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.paymentsystemproject.domain.cartitem.service.CartService;
import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.order.entity.OrderStatus;
import com.paymentsystemproject.domain.payment.dto.PaymentConfirmResponseDto;
import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.domain.payment.entity.PaymentStatus;
import com.paymentsystemproject.domain.payment.repository.PaymentRepository;
import com.paymentsystemproject.domain.point.service.PointService;
import com.paymentsystemproject.global.error.BusinessException;

@ExtendWith(MockitoExtension.class)
class PaymentCommandServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    CartService cartService;

    @Mock
    PointService pointService;

    @InjectMocks
    PaymentCommandService paymentCommandService;

    @Test
    @DisplayName("결제 실패 처리 성공 - PENDING 결제를 FAILED로 변경하고 주문 취소")
    void failPayment_success() {
        // given
        Long orderId = 1L;

        Payment payment = mock(Payment.class);
        Order order = mock(Order.class);

        when(paymentRepository.findByOrderIdWithOrderForUpdate(orderId))
            .thenReturn(Optional.of(payment));

        when(payment.getOrder()).thenReturn(order);
        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);

        // when
        paymentCommandService.failPayment(orderId);

        // then
        verify(payment).markAsFailed();
        verify(order).cancelPendingOrder();
    }

    @Test
    @DisplayName("이미 FAILED 상태이면 결제 실패 처리를 다시 하지 않는다")
    void failPayment_alreadyFailed_return() {
        // given
        Long orderId = 1L;

        Payment payment = mock(Payment.class);
        Order order = mock(Order.class);

        when(paymentRepository.findByOrderIdWithOrderForUpdate(orderId))
            .thenReturn(Optional.of(payment));

        when(payment.getOrder()).thenReturn(order);
        when(payment.getStatus()).thenReturn(PaymentStatus.FAILED);

        // when
        paymentCommandService.failPayment(orderId);

        // then
        verify(payment, never()).markAsFailed();
        verify(order, never()).cancelPendingOrder();
    }

    @Test
    @DisplayName("PENDING이 아닌 결제는 실패 처리할 수 없다")
    void failPayment_alreadyProcessed_fail() {
        // given
        Long orderId = 1L;

        Payment payment = mock(Payment.class);
        Order order = mock(Order.class);

        when(paymentRepository.findByOrderIdWithOrderForUpdate(orderId))
            .thenReturn(Optional.of(payment));

        when(payment.getOrder()).thenReturn(order);
        when(payment.getStatus()).thenReturn(PaymentStatus.COMPLETED);

        // when & then
        assertThatThrownBy(() -> paymentCommandService.failPayment(orderId))
            .isInstanceOf(BusinessException.class);

        verify(payment, never()).markAsFailed();
        verify(order, never()).cancelPendingOrder();
    }

    @Test
    @DisplayName("결제를 찾을 수 없으면 실패 처리 예외 발생")
    void failPayment_paymentNotFound_fail() {
        // given
        Long orderId = 1L;

        when(paymentRepository.findByOrderIdWithOrderForUpdate(orderId))
            .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentCommandService.failPayment(orderId))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("사용자 결제 취소 성공 - PENDING 결제를 CANCELED로 변경하고 주문 취소")
    void cancelPayment_success() {
        // given
        Long orderId = 1L;

        Payment payment = mock(Payment.class);
        Order order = mock(Order.class);

        when(paymentRepository.findByOrderIdWithOrderForUpdate(orderId))
            .thenReturn(Optional.of(payment));

        when(payment.getOrder()).thenReturn(order);
        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);

        // when
        paymentCommandService.cancelPayment(orderId);

        // then
        verify(payment).markAsCanceled();
        verify(order).cancelPendingOrder();
    }

    @Test
    @DisplayName("이미 CANCELED 상태이면 결제 취소 처리를 다시 하지 않는다")
    void cancelPayment_alreadyCanceled_return() {
        // given
        Long orderId = 1L;

        Payment payment = mock(Payment.class);
        Order order = mock(Order.class);

        when(paymentRepository.findByOrderIdWithOrderForUpdate(orderId))
            .thenReturn(Optional.of(payment));

        when(payment.getOrder()).thenReturn(order);
        when(payment.getStatus()).thenReturn(PaymentStatus.CANCELED);

        // when
        paymentCommandService.cancelPayment(orderId);

        // then
        verify(payment, never()).markAsCanceled();
        verify(order, never()).cancelPendingOrder();
    }

    @Test
    @DisplayName("PENDING이 아닌 결제는 취소 처리할 수 없다")
    void cancelPayment_alreadyProcessed_fail() {
        // given
        Long orderId = 1L;

        Payment payment = mock(Payment.class);
        Order order = mock(Order.class);

        when(paymentRepository.findByOrderIdWithOrderForUpdate(orderId))
            .thenReturn(Optional.of(payment));

        when(payment.getOrder()).thenReturn(order);
        when(payment.getStatus()).thenReturn(PaymentStatus.COMPLETED);

        // when & then
        assertThatThrownBy(() -> paymentCommandService.cancelPayment(orderId))
            .isInstanceOf(BusinessException.class);

        verify(payment, never()).markAsCanceled();
        verify(order, never()).cancelPendingOrder();
    }

    @Test
    @DisplayName("결제 완료 처리 성공 - 주문 완료, 포인트 차감/적립, 원장 기록, 장바구니 초기화")
    void completePayment_success() {
        // given
        Long orderId = 1L;
        Long memberId = 10L;

        Payment payment = mock(Payment.class);
        Order order = mock(Order.class);
        Member member = mock(Member.class);

        when(paymentRepository.findByOrderIdWithOrderForUpdate(orderId))
            .thenReturn(Optional.of(payment));

        when(payment.getOrder()).thenReturn(order);
        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);

        when(order.getMember()).thenReturn(member);
        when(order.getStatus()).thenReturn(OrderStatus.COMPLETED);
        when(member.getId()).thenReturn(memberId);

        when(payment.getUsePoint()).thenReturn(3000);
        when(payment.getPgAmount()).thenReturn(10000);

        // when
        PaymentConfirmResponseDto result =
            paymentCommandService.completePayment(orderId);

        // then
        assertThat(result).isNotNull();

        verify(order).complete();

        verify(member).decreasePoint(3000);
        verify(pointService).recordUsePoint(member, payment, 3000);

        verify(member).increasePoint(100);
        verify(pointService).recordEarnPoint(member, payment, 100);

        verify(payment).markAsCompleted(100);
        verify(cartService).removeCart(memberId);
    }

    @Test
    @DisplayName("PG 결제 금액이 0원이면 적립 포인트는 0원")
    void completePayment_pgAmountZero_earnedPointZero() {
        // given
        Long orderId = 1L;
        Long memberId = 10L;

        Payment payment = mock(Payment.class);
        Order order = mock(Order.class);
        Member member = mock(Member.class);

        when(paymentRepository.findByOrderIdWithOrderForUpdate(orderId))
            .thenReturn(Optional.of(payment));

        when(payment.getOrder()).thenReturn(order);
        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);

        when(order.getMember()).thenReturn(member);
        when(order.getStatus()).thenReturn(OrderStatus.COMPLETED);
        when(member.getId()).thenReturn(memberId);

        when(payment.getUsePoint()).thenReturn(5000);
        when(payment.getPgAmount()).thenReturn(0);

        // when
        PaymentConfirmResponseDto result =
            paymentCommandService.completePayment(orderId);

        // then
        assertThat(result).isNotNull();

        verify(order).complete();

        verify(member).decreasePoint(5000);
        verify(pointService).recordUsePoint(member, payment, 5000);

        verify(member).increasePoint(0);
        verify(pointService).recordEarnPoint(member, payment, 0);

        verify(payment).markAsCompleted(0);
        verify(cartService).removeCart(memberId);
    }

    @Test
    @DisplayName("이미 COMPLETED 상태이면 멱등 응답을 반환하고 상태 변경을 다시 하지 않는다")
    void completePayment_alreadyCompleted_return() {
        // given
        Long orderId = 1L;

        Payment payment = mock(Payment.class);
        Order order = mock(Order.class);

        when(paymentRepository.findByOrderIdWithOrderForUpdate(orderId))
            .thenReturn(Optional.of(payment));

        when(payment.getOrder()).thenReturn(order);
        when(payment.getStatus()).thenReturn(PaymentStatus.COMPLETED);
        when(order.getStatus()).thenReturn(OrderStatus.COMPLETED);

        // when
        PaymentConfirmResponseDto result =
            paymentCommandService.completePayment(orderId);

        // then
        assertThat(result).isNotNull();

        verify(order, never()).complete();
        verify(payment, never()).markAsCompleted(anyInt());
        verify(pointService, never()).recordUsePoint(any(), any(), anyInt());
        verify(pointService, never()).recordEarnPoint(any(), any(), anyInt());
        verify(cartService, never()).removeCart(anyLong());
    }

    @Test
    @DisplayName("PENDING이 아닌 결제는 완료 처리할 수 없다")
    void completePayment_alreadyProcessed_fail() {
        // given
        Long orderId = 1L;

        Payment payment = mock(Payment.class);
        Order order = mock(Order.class);

        when(paymentRepository.findByOrderIdWithOrderForUpdate(orderId))
            .thenReturn(Optional.of(payment));

        when(payment.getOrder()).thenReturn(order);
        when(payment.getStatus()).thenReturn(PaymentStatus.CANCELED);

        // when & then
        assertThatThrownBy(() -> paymentCommandService.completePayment(orderId))
            .isInstanceOf(BusinessException.class);

        verify(order, never()).complete();
        verify(payment, never()).markAsCompleted(anyInt());
        verify(cartService, never()).removeCart(anyLong());
    }

    @Test
    @DisplayName("전액 포인트 결제 성공 - 적립 없이 포인트 차감, 원장 기록, 장바구니 초기화")
    void confirmPointOnlyPayment_success() {
        // given
        Long memberId = 10L;

        Payment payment = mock(Payment.class);
        Order order = mock(Order.class);
        Member member = mock(Member.class);

        when(payment.getOrder()).thenReturn(order);
        when(payment.getStatus()).thenReturn(PaymentStatus.COMPLETED);

        when(order.getMember()).thenReturn(member);
        when(order.getStatus()).thenReturn(OrderStatus.COMPLETED);

        when(member.getId()).thenReturn(memberId);
        when(payment.getUsePoint()).thenReturn(10000);

        // when
        PaymentConfirmResponseDto result =
            paymentCommandService.confirmPointOnlyPayment(payment);

        // then
        assertThat(result).isNotNull();

        verify(order).complete();
        verify(payment).markAsCompleted(0);

        verify(member).decreasePoint(10000);
        verify(pointService).recordUsePoint(member, payment, 10000);

        verify(pointService, never()).recordEarnPoint(any(), any(), anyInt());
        verify(member, never()).increasePoint(anyInt());

        verify(cartService).removeCart(memberId);
    }
}
