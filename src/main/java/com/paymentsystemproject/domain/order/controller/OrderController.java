package com.paymentsystemproject.domain.order.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.domain.order.dto.CreateOrderRequestDto;
import com.paymentsystemproject.domain.order.dto.CreateOrderResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderListResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderPreviewResponseDto;
import com.paymentsystemproject.domain.order.service.OrderService;
import com.paymentsystemproject.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<GetOrderPreviewResponseDto>> getOrderPreview(
        @RequestParam Long memberId,
        @RequestParam(required = false) List<Long> cartItemIds) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.ok(orderService.getOrderPreview(memberId, cartItemIds)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateOrderResponseDto>> createOrder(
        @RequestParam Long memberId,
        @RequestBody CreateOrderRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(orderService.createOrder(memberId, requestDto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<GetOrderListResponseDto>>> getOrderList(
        @RequestParam Long memberId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.ok(orderService.getOrderList(memberId, page, size)));
    }
}
