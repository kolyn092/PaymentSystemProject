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
import com.paymentsystemproject.domain.order.facade.OrderFacade;
import com.paymentsystemproject.global.response.ApiResponse;
import com.paymentsystemproject.global.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 주문 관련 API 요청을 처리하는 컨트롤러입니다.
 * 주문서 미리보기, 주문 생성, 주문 목록/상세 조회, 주문 취소 API를 제공합니다.
 * 모든 요청은 JWT 인증을 통해 로그인한 회원만 접근 가능합니다.
 */

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderFacade orderFacade;

    /**
     * 주문서 미리보기 조회
     * GET /api/orders/preview
     *
     * @param userDetails 로그인한 회원 정보
     * @param cartItemIds 결제할 장바구니 항목 ID 목록 (미입력 시 전체 장바구니 기준)
     * @return 주문서 정보 (상품 목록, 총액, 사용 가능 포인트)
     */

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<GetOrderPreviewResponseDto>> getOrderPreview(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(required = false) List<Long> cartItemIds) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.ok(orderFacade.getOrderPreview(userDetails.getMemberId(), cartItemIds)));
    }

    /**
     * 주문 생성
     * POST /api/orders
     *
     * @param userDetails 로그인한 회원 정보
     * @param requestDto 주문할 장바구니 항목 ID 목록 및 사용할 포인트
     * @return 생성된 주문 정보 및 결제 준비 정보
     */

    @PostMapping
    public ResponseEntity<ApiResponse<CreateOrderResponseDto>> createOrder(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody CreateOrderRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(orderFacade.createOrder(userDetails.getMemberId(), requestDto)));
    }

    /**
     * 내 주문 목록 조회
     * GET /api/orders
     *
     * @param userDetails 로그인한 회원 정보
     * @param page 페이지 번호 (기본값 0)
     * @param size 페이지당 항목 수 (기본값 20)
     * @return 주문 목록 (페이징)
     */

    @GetMapping
    public ResponseEntity<ApiResponse<Page<GetOrderListResponseDto>>> getOrderList(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.ok(orderService.getOrderList(userDetails.getMemberId(), page, size)));
    }

    /**
     * 주문 상세 조회
     * GET /api/orders/{orderId}
     *
     * @param userDetails 로그인한 회원 정보
     * @param orderId 조회할 주문 ID
     * @return 주문 상세 정보 및 결제 정보
     */

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<GetOrderDetailResponseDto>> getOrderDetail(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long orderId) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.ok(orderService.getOrderDetail(userDetails.getMemberId(), orderId)));
    }

    /**
     * 주문 취소
     * POST /api/orders/{orderId}/cancel
     *
     * @param userDetails 로그인한 회원 정보
     * @param orderId 취소할 주문 ID
     * @return 취소된 주문 정보
     */

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<CancelOrderResponseDto>> cancelOrder(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long orderId) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.ok(orderService.cancelOrder(userDetails.getMemberId(), orderId)));
    }
}
