package com.paymentsystemproject.domain.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.domain.payment.dto.PaymentConfirmRequestDto;
import com.paymentsystemproject.domain.payment.dto.PaymentConfirmResponseDto;
import com.paymentsystemproject.domain.payment.facade.PaymentFacade;
import com.paymentsystemproject.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentFacade paymentFacade;

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentConfirmResponseDto>> confirmPayment(
        @AuthenticationPrincipal Long memberId,
        @Valid @RequestBody PaymentConfirmRequestDto request) {
        return ResponseEntity.ok(ApiResponse.ok(paymentFacade.confirmPayment(memberId, request)));
    }

}
