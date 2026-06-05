package com.paymentsystemproject.domain.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * PortOne 결제 취소 API를 호출하는 Service.
 *
 * 역할:
 * - DB 환불 처리가 커밋된 뒤 실제 PG 취소 요청을 보낸다.
 * - PG 환불 금액이 0원이면 호출하지 않는다.
 */
@Slf4j
@Service
public class PortOneRefundService {

    @Value("${portone.base-url:https://api.portone.io}")
    private String portOneBaseUrl;

    @Value("${portone.api-secret:}")
    private String portOneApiSecret;

    /**
     * PortOne 결제 취소 API를 호출한다.
     *
     * @param portonePaymentId PortOne 결제 ID
     * @param amount PG 취소 금액
     * @param currentCancellableAmount 현재 취소 가능 금액
     * @param reason 취소 사유
     */
    public void cancelPayment(
        String portonePaymentId,
        Integer amount,
        Integer currentCancellableAmount,
        String reason
    ) {
        if (amount == null || amount <= 0) {
            return;
        }

        if (portOneApiSecret == null || portOneApiSecret.isBlank()) {
            throw new BusinessException(ErrorCode.PORTONE_SECRET_NOT_FOUND);
        }

        PortOneCancelPaymentRequest request = new PortOneCancelPaymentRequest(
            reason,
            amount,
            currentCancellableAmount
        );

        RestClient restClient = RestClient.builder()
            .baseUrl(portOneBaseUrl)
            .build();

        restClient.post()
            .uri("/payments/{paymentId}/cancel", portonePaymentId)
            .header(HttpHeaders.AUTHORIZATION, "PortOne " + portOneApiSecret)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toBodilessEntity();

        log.info(
            "PortOne PG 환불 요청 성공. paymentId={}, amount={}",
            portonePaymentId,
            amount
        );
    }

    private record PortOneCancelPaymentRequest(
        String reason,
        Integer amount,
        Integer currentCancellableAmount
    ) {
    }
}
