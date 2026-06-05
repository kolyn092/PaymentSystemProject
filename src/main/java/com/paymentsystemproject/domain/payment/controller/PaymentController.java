package com.paymentsystemproject.domain.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.domain.payment.dto.PaymentConfirmRequestDto;
import com.paymentsystemproject.domain.payment.dto.PaymentConfirmResponseDto;
import com.paymentsystemproject.domain.payment.facade.PaymentFacade;
import com.paymentsystemproject.global.response.ApiResponse;
import com.paymentsystemproject.global.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentFacade paymentFacade;

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentConfirmResponseDto>> confirmPayment(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody PaymentConfirmRequestDto request) {
        return ResponseEntity.ok(ApiResponse.ok(paymentFacade.confirmPayment(userDetails.getMemberId(), request)));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelPayment(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long orderId
    ) {
        paymentFacade.cancelPayment(userDetails.getMemberId(), orderId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

}
