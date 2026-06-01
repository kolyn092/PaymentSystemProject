package com.paymentsystemproject.domain.point.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.domain.point.dto.GetPointBalanceResponseDto;
import com.paymentsystemproject.domain.point.service.PointService;
import com.paymentsystemproject.global.response.ApiResponse;
import com.paymentsystemproject.global.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<GetPointBalanceResponseDto>> getPointBalance(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = userDetails.getMemberId();
        GetPointBalanceResponseDto responseDto = pointService.getPointBalance(memberId);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.ok(responseDto));
    }
}
