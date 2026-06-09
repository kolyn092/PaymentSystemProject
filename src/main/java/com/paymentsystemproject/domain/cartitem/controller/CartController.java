package com.paymentsystemproject.domain.cartitem.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.domain.cartitem.dto.AddCartRequestDto;
import com.paymentsystemproject.domain.cartitem.dto.AddCartResponseDto;
import com.paymentsystemproject.domain.cartitem.dto.GetCartResponseDto;
import com.paymentsystemproject.domain.cartitem.dto.UpdateCartRequestDto;
import com.paymentsystemproject.domain.cartitem.service.CartService;
import com.paymentsystemproject.global.response.ApiResponse;
import com.paymentsystemproject.global.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cartitems")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<ApiResponse<AddCartResponseDto>> addItem(
        @AuthenticationPrincipal CustomUserDetails userDetails, @Valid @RequestBody AddCartRequestDto request) {
        AddCartResponseDto responseDto = cartService.addItem(userDetails.getMemberId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(responseDto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<GetCartResponseDto>> getItems(
        @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.ok(cartService.getCartItems(userDetails.getMemberId())));
    }

    @GetMapping("/selected")
    public ResponseEntity<ApiResponse<GetCartResponseDto>> getSelectedItems(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(name = "ids", required = false) List<Long> cartItemIds) {

        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.ok(cartService.getSelectedItems(userDetails.getMemberId(), cartItemIds)));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<String>> updateQuantity(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody UpdateCartRequestDto request) {

        cartService.updateQuantity(userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.ok("수량이 성공적으로 변경되었습니다."));
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<String>> removeItem(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long cartItemId) {

        cartService.removeItem(userDetails.getMemberId(), cartItemId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.ok("상품이 장바구니에서 삭제되었습니다."));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> removeCart(
        @AuthenticationPrincipal CustomUserDetails userDetails) {

        cartService.removeCart(userDetails.getMemberId());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.ok("장바구니가 모두 비워졌습니다."));
    }

}
