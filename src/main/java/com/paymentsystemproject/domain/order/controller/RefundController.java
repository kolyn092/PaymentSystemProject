package com.paymentsystemproject.domain.order.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.domain.order.dto.CreateRefundRequestDto;
import com.paymentsystemproject.domain.order.dto.CreateRefundResponseDto;
import com.paymentsystemproject.domain.order.facade.RefundFacade;
import com.paymentsystemproject.global.response.ApiResponse;
import com.paymentsystemproject.global.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class RefundController {

    private final RefundFacade refundFacade;

    @PostMapping("/{orderId}/refunds")
    public ResponseEntity<ApiResponse<CreateRefundResponseDto>> createRefund(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long orderId,
        @Valid @RequestBody CreateRefundRequestDto requestDto
    ) {
        Long memberId = userDetails.getMemberId();

        CreateRefundResponseDto responseDto = refundFacade.createRefund(
            memberId,
            orderId,
            requestDto
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(responseDto));
    }
}
