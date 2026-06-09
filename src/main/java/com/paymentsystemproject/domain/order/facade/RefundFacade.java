package com.paymentsystemproject.domain.order.facade;

import org.springframework.stereotype.Component;

import com.paymentsystemproject.domain.order.dto.CreateRefundRequestDto;
import com.paymentsystemproject.domain.order.dto.CreateRefundResponseDto;
import com.paymentsystemproject.domain.order.service.RefundService;
import com.paymentsystemproject.domain.payment.service.PortOneRefundService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 환불 유스케이스의 전체 흐름을 조립하는 Facade.
 *
 * 역할:
 * - Controller와 Service 사이에서 환불 전체 흐름을 담당한다.
 * - RefundService의 세부 기능들을 조합한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundFacade {

    private final RefundService refundService;
    private final PortOneRefundService portOneRefundService;

    public CreateRefundResponseDto createRefund(
        Long memberId,
        Long orderId,
        CreateRefundRequestDto requestDto
    ) {
        RefundService.RefundProcessResult result = refundService.processRefund(
            memberId,
            orderId,
            requestDto
        );

        tryCancelPgRefund(result);

        return result.responseDto();
    }

    private void tryCancelPgRefund(RefundService.RefundProcessResult result) {
        if (!result.hasPgRefundAmount()) {
            return;
        }

        try {
            portOneRefundService.cancelPayment(
                result.portonePaymentId(),
                result.pgRefundAmount(),
                result.currentCancellableAmount(),
                result.reason()
            );
        } catch (Exception e) {
            log.error(
                "PG 환불 요청 실패. refundId={}, paymentId={}, amount={}, message={}",
                result.refundId(),
                result.portonePaymentId(),
                result.pgRefundAmount(),
                e.getMessage()
            );
        }
    }
}
