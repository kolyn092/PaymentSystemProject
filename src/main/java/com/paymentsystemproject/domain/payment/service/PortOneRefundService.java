package com.paymentsystemproject.domain.payment.service;

import org.springframework.stereotype.Service;

import com.paymentsystemproject.domain.infra.portone.client.PortOneClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortOneRefundService {

    private final PortOneClient portOneClient;

    public void cancelPayment(
        String portonePaymentId,
        Integer amount,
        Integer currentCancellableAmount,
        String reason
    ) {
        if (amount == null || amount <= 0) {
            return;
        }

        portOneClient.cancelPayment(
            portonePaymentId,
            reason,
            amount,
            currentCancellableAmount
        );

        log.info(
            "PortOne PG 환불 요청 성공. paymentId={}, amount={}",
            portonePaymentId,
            amount
        );
    }
}
