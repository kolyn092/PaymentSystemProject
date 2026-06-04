package com.paymentsystemproject.domain.order.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.domain.order.dto.CancelOrderResponseDto;
import com.paymentsystemproject.domain.order.dto.CreateOrderRequestDto;
import com.paymentsystemproject.domain.order.dto.CreateOrderResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderDetailResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderListResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderPreviewResponseDto;
import com.paymentsystemproject.domain.order.service.OrderService;
import com.paymentsystemproject.global.response.ApiResponse;
import com.paymentsystemproject.global.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<GetOrderPreviewResponseDto>> getOrderPreview(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(required = false) List<Long> cartItemIds) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.ok(orderService.getOrderPreview(userDetails.getMemberId(), cartItemIds)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateOrderResponseDto>> createOrder(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody CreateOrderRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(orderService.createOrder(userDetails.getMemberId(), requestDto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<GetOrderListResponseDto>>> getOrderList(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.ok(orderService.getOrderList(userDetails.getMemberId(), page, size)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<GetOrderDetailResponseDto>> getOrderDetail(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long orderId) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.ok(orderService.getOrderDetail(userDetails.getMemberId(), orderId)));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<CancelOrderResponseDto>> cancelOrder(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long orderId) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.ok(orderService.cancelOrder(userDetails.getMemberId(), orderId)));
    }
}
