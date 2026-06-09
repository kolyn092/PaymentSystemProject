package com.paymentsystemproject.domain.payment.facade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.order.entity.OrderStatus;
import com.paymentsystemproject.domain.payment.dto.PaymentConfirmRequestDto;
import com.paymentsystemproject.domain.payment.dto.PaymentConfirmResponseDto;
import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.domain.payment.entity.PaymentStatus;
import com.paymentsystemproject.domain.payment.port.PaymentGateway;
import com.paymentsystemproject.domain.payment.port.PaymentGatewayResponseDto;
import com.paymentsystemproject.domain.payment.service.PaymentCommandService;
import com.paymentsystemproject.domain.payment.service.PaymentService;
import com.paymentsystemproject.global.error.BusinessException;

@ExtendWith(MockitoExtension.class)
class PaymentFacadeTest {

    @Mock
    PaymentService paymentService;

    @Mock
    PaymentCommandService paymentCommandService;

    @Mock
    PaymentGateway paymentGateway;

    @InjectMocks
    PaymentFacade paymentFacade;

    @Test
    @DisplayName("PG 결제 승인 성공")
    void confirmPayment_success() {
        // given
        Long memberId = 1L;
        Long orderId = 10L;
        String portonePaymentId = "payment-123";

        PaymentConfirmRequestDto request =
            new PaymentConfirmRequestDto(orderId, portonePaymentId);

        Order order = mock(Order.class);
        Payment payment = mock(Payment.class);

        when(paymentService.findByOrderIdAndMemberId(orderId, memberId))
            .thenReturn(payment);

        when(payment.getOrder()).thenReturn(order);
        when(payment.getPortonePaymentId()).thenReturn(portonePaymentId);
        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);
        when(payment.isPointOnlyPayment()).thenReturn(false);
        when(payment.getPgAmount()).thenReturn(10000);
        when(order.getStatus()).thenReturn(OrderStatus.PENDING_PAYMENT);
        when(order.getId()).thenReturn(orderId);

        PaymentGatewayResponseDto pgResponse =
            new PaymentGatewayResponseDto(portonePaymentId, "PAID", 10000);

        when(paymentGateway.getPayment(portonePaymentId))
            .thenReturn(pgResponse);

        PaymentConfirmResponseDto expectedResponse =
            mock(PaymentConfirmResponseDto.class);

        when(paymentCommandService.completePayment(orderId))
            .thenReturn(expectedResponse);

        // when
        PaymentConfirmResponseDto result =
            paymentFacade.confirmPayment(memberId, request);

        // then
        assertThat(result).isSameAs(expectedResponse);

        verify(paymentGateway).getPayment(portonePaymentId);
        verify(paymentCommandService).completePayment(orderId);
        verify(paymentCommandService, never()).failPayment(anyLong());
        verify(paymentGateway, never()).cancelPayment(any(), any(), any());
    }

    @Test
    @DisplayName("전액 포인트 결제이면 PG 조회 없이 포인트 결제 확정 처리")
    void confirmPayment_pointOnly_success() {
        // given
        Long memberId = 1L;
        Long orderId = 10L;
        String portonePaymentId = "point-only-payment";

        PaymentConfirmRequestDto request =
            new PaymentConfirmRequestDto(orderId, portonePaymentId);

        Order order = mock(Order.class);
        Payment payment = mock(Payment.class);

        when(paymentService.findByOrderIdAndMemberId(orderId, memberId))
            .thenReturn(payment);

        when(payment.getOrder()).thenReturn(order);
        when(payment.getPortonePaymentId()).thenReturn(portonePaymentId);
        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);
        when(payment.isPointOnlyPayment()).thenReturn(true);
        when(order.getStatus()).thenReturn(OrderStatus.PENDING_PAYMENT);

        PaymentConfirmResponseDto expectedResponse =
            mock(PaymentConfirmResponseDto.class);

        when(paymentCommandService.confirmPointOnlyPayment(payment))
            .thenReturn(expectedResponse);

        // when
        PaymentConfirmResponseDto result =
            paymentFacade.confirmPayment(memberId, request);

        // then
        assertThat(result).isSameAs(expectedResponse);

        verify(paymentCommandService).confirmPointOnlyPayment(payment);
        verify(paymentGateway, never()).getPayment(any());
        verify(paymentCommandService, never()).completePayment(anyLong());
    }

    @Test
    @DisplayName("portonePaymentId가 일치하지 않으면 예외 발생")
    void confirmPayment_portonePaymentIdMismatch_fail() {
        // given
        Long memberId = 1L;
        Long orderId = 10L;

        PaymentConfirmRequestDto request =
            new PaymentConfirmRequestDto(orderId, "request-payment-id");

        Order order = mock(Order.class);
        Payment payment = mock(Payment.class);

        when(paymentService.findByOrderIdAndMemberId(orderId, memberId))
            .thenReturn(payment);

        when(payment.getOrder()).thenReturn(order);
        when(payment.getPortonePaymentId()).thenReturn("db-payment-id");

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirmPayment(memberId, request))
            .isInstanceOf(BusinessException.class);

        verify(paymentGateway, never()).getPayment(any());
        verify(paymentCommandService, never()).completePayment(anyLong());
    }

    @Test
    @DisplayName("이미 실패한 결제는 재승인할 수 없다")
    void confirmPayment_alreadyFailed_fail() {
        // given
        Long memberId = 1L;
        Long orderId = 10L;
        String portonePaymentId = "payment-123";

        PaymentConfirmRequestDto request =
            new PaymentConfirmRequestDto(orderId, portonePaymentId);

        Order order = mock(Order.class);
        Payment payment = mock(Payment.class);

        when(paymentService.findByOrderIdAndMemberId(orderId, memberId))
            .thenReturn(payment);

        when(payment.getOrder()).thenReturn(order);
        when(payment.getPortonePaymentId()).thenReturn(portonePaymentId);
        when(payment.getStatus()).thenReturn(PaymentStatus.FAILED);

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirmPayment(memberId, request))
            .isInstanceOf(BusinessException.class);

        verify(paymentGateway, never()).getPayment(any());
        verify(paymentCommandService, never()).completePayment(anyLong());
    }

    @Test
    @DisplayName("주문 상태가 결제 대기가 아니면 예외 발생")
    void confirmPayment_invalidOrderStatus_fail() {
        // given
        Long memberId = 1L;
        Long orderId = 10L;
        String portonePaymentId = "payment-123";

        PaymentConfirmRequestDto request =
            new PaymentConfirmRequestDto(orderId, portonePaymentId);

        Order order = mock(Order.class);
        Payment payment = mock(Payment.class);

        when(paymentService.findByOrderIdAndMemberId(orderId, memberId))
            .thenReturn(payment);

        when(payment.getOrder()).thenReturn(order);
        when(payment.getPortonePaymentId()).thenReturn(portonePaymentId);
        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);
        when(order.getStatus()).thenReturn(OrderStatus.COMPLETED);

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirmPayment(memberId, request))
            .isInstanceOf(BusinessException.class);

        verify(paymentGateway, never()).getPayment(any());
        verify(paymentCommandService, never()).completePayment(anyLong());
    }

    @Test
    @DisplayName("PG 결제 상태가 PAID가 아니면 결제 실패 처리")
    void confirmPayment_pgStatusNotPaid_fail() {
        // given
        Long memberId = 1L;
        Long orderId = 10L;
        String portonePaymentId = "payment-123";

        PaymentConfirmRequestDto request =
            new PaymentConfirmRequestDto(orderId, portonePaymentId);

        Order order = mock(Order.class);
        Payment payment = mock(Payment.class);

        when(paymentService.findByOrderIdAndMemberId(orderId, memberId))
            .thenReturn(payment);

        when(payment.getOrder()).thenReturn(order);
        when(payment.getPortonePaymentId()).thenReturn(portonePaymentId);
        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);
        when(payment.isPointOnlyPayment()).thenReturn(false);
        when(order.getStatus()).thenReturn(OrderStatus.PENDING_PAYMENT);
        when(order.getId()).thenReturn(orderId);

        PaymentGatewayResponseDto pgResponse =
            new PaymentGatewayResponseDto(portonePaymentId, "FAILED", 10000);

        when(paymentGateway.getPayment(portonePaymentId))
            .thenReturn(pgResponse);

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirmPayment(memberId, request))
            .isInstanceOf(BusinessException.class);

        verify(paymentCommandService).failPayment(orderId);
        verify(paymentCommandService, never()).completePayment(anyLong());
        verify(paymentGateway, never()).cancelPayment(any(), any(), any());
    }

    @Test
    @DisplayName("PG 승인 금액과 서버 PG 결제 금액이 다르면 PG 취소 후 실패 처리")
    void confirmPayment_amountMismatch_fail() {
        // given
        Long memberId = 1L;
        Long orderId = 10L;
        String portonePaymentId = "payment-123";

        PaymentConfirmRequestDto request =
            new PaymentConfirmRequestDto(orderId, portonePaymentId);

        Order order = mock(Order.class);
        Payment payment = mock(Payment.class);

        when(paymentService.findByOrderIdAndMemberId(orderId, memberId))
            .thenReturn(payment);

        when(payment.getOrder()).thenReturn(order);
        when(payment.getPortonePaymentId()).thenReturn(portonePaymentId);
        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);
        when(payment.isPointOnlyPayment()).thenReturn(false);
        when(payment.getPgAmount()).thenReturn(10000);
        when(order.getStatus()).thenReturn(OrderStatus.PENDING_PAYMENT);
        when(order.getId()).thenReturn(orderId);

        PaymentGatewayResponseDto pgResponse =
            new PaymentGatewayResponseDto(portonePaymentId, "PAID", 9000);

        when(paymentGateway.getPayment(portonePaymentId))
            .thenReturn(pgResponse);

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirmPayment(memberId, request))
            .isInstanceOf(BusinessException.class);

        verify(paymentGateway).cancelPayment(
            eq(portonePaymentId),
            eq("결제 금액 불일치 자동 취소"),
            isNull()
        );

        verify(paymentCommandService).failPayment(orderId);
        verify(paymentCommandService, never()).completePayment(anyLong());
    }

    @Test
    @DisplayName("사용자 결제 취소 - PG 결제이면 내부 취소 후 PG 취소 요청")
    void cancelPayment_pgPayment_success() {
        // given
        Long memberId = 1L;
        Long orderId = 10L;
        String portonePaymentId = "payment-123";

        Order order = mock(Order.class);
        Payment payment = mock(Payment.class);

        when(paymentService.findByOrderIdAndMemberId(orderId, memberId))
            .thenReturn(payment);

        when(payment.getOrder()).thenReturn(order);
        when(order.getId()).thenReturn(orderId);
        when(payment.isPointOnlyPayment()).thenReturn(false);
        when(payment.getPortonePaymentId()).thenReturn(portonePaymentId);

        // when
        paymentFacade.cancelPayment(memberId, orderId);

        // then
        verify(paymentCommandService).cancelPayment(orderId);
        verify(paymentGateway).cancelPayment(
            eq(portonePaymentId),
            eq("사용자 결제 취소"),
            isNull()
        );
    }

    @Test
    @DisplayName("사용자 결제 취소 - 전액 포인트 결제이면 PG 취소 요청하지 않음")
    void cancelPayment_pointOnly_success() {
        // given
        Long memberId = 1L;
        Long orderId = 10L;

        Order order = mock(Order.class);
        Payment payment = mock(Payment.class);

        when(paymentService.findByOrderIdAndMemberId(orderId, memberId))
            .thenReturn(payment);

        when(payment.getOrder()).thenReturn(order);
        when(order.getId()).thenReturn(orderId);
        when(payment.isPointOnlyPayment()).thenReturn(true);

        // when
        paymentFacade.cancelPayment(memberId, orderId);

        // then
        verify(paymentCommandService).cancelPayment(orderId);
        verify(paymentGateway, never()).cancelPayment(any(), any(), any());
    }
}
