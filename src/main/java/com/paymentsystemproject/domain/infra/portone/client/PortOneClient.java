package com.paymentsystemproject.domain.infra.portone.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.paymentsystemproject.domain.infra.portone.config.PortOneProperties;
import com.paymentsystemproject.domain.infra.portone.dto.PortOneCancelRequestDto;
import com.paymentsystemproject.domain.infra.portone.dto.PortOnePaymentResponseDto;
import com.paymentsystemproject.domain.payment.port.PaymentGateway;
import com.paymentsystemproject.domain.payment.port.PaymentGatewayResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortOneClient implements PaymentGateway {

    private final RestClient portOneRestClient;
    private final PortOneProperties portOneProperties;

    @Override
    public PaymentGatewayResponseDto getPayment(String paymentId) {
        // paymentId logging
        log.info("PortOne 결제 조회: {}", paymentId);

        // portOneRestClient https://api.portone.io/payments/{paymnetId}?storeId={storeId}
        PortOnePaymentResponseDto response = portOneRestClient.get()
            .uri(uriBuilder -> uriBuilder.path("/payments/{paymentId}")
                .queryParam("storeId", portOneProperties.getStoreId())
                .build(paymentId))
            .retrieve()
            .body(PortOnePaymentResponseDto.class);

        // PortOne 응답 결과인 PortOnePaymentResponse를 PaymentGatewayResponse로 변환
        return response.toGatewayResponse();
    }

    @Override
    public void cancelPayment(String paymentId, String reason, Integer amount) {
        cancelPayment(paymentId, reason, amount, null);
    }

    public void cancelPayment(String paymentId, String reason, Integer amount, Integer currentCancellableAmount) {
        log.info("PortOne 결제 취소 요청: paymentId={}, amount={}, reason={}", paymentId, amount, reason);

        PortOneCancelRequestDto requestDto = new PortOneCancelRequestDto(
            reason,
            amount,
            currentCancellableAmount,
            portOneProperties.getStoreId()
        );

        portOneRestClient.post()
            .uri("/payments/{paymentId}/cancel", paymentId)
            .body(requestDto)
            .retrieve()
            .toBodilessEntity();
    }
}
