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
import com.paymentsystemproject.domain.cartitem.dto.GetCartItemResponseDto;
import com.paymentsystemproject.domain.cartitem.dto.UpdateCartRequestDto;
import com.paymentsystemproject.domain.cartitem.service.CartService;
import com.paymentsystemproject.global.security.CustomUserDetails;
import com.paymentsystemproject.global.security.jwt.JwtTokenProvider;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cartitems")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping
    public ResponseEntity<AddCartResponseDto> addItem(@Valid @RequestBody AddCartRequestDto request) {
        Long savedItem = cartService.addItem(request);

        AddCartResponseDto responseDto = new AddCartResponseDto(savedItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping
    public ResponseEntity<List<GetCartItemResponseDto>> getItems(
        @AuthenticationPrincipal CustomUserDetails userDetails) { // 👈 변경!

        // userDetails에서 getMemberId()로 꺼내서 넘겨줍니다.
        return ResponseEntity.status(HttpStatus.OK)
            .body(cartService.getCartItems(userDetails.getMemberId()));
    }

    @GetMapping("/selected")
    public ResponseEntity<List<GetCartItemResponseDto>> getSelectedItems(
        @AuthenticationPrincipal CustomUserDetails userDetails, // 👈 변경!
        @RequestParam(name = "ids", required = false) List<Long> cartItemIds) {

        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(List.of());
        }

        List<GetCartItemResponseDto> responseDtoList =
            cartService.getSelectedItems(userDetails.getMemberId(), cartItemIds);
        return ResponseEntity.status(HttpStatus.OK).body(responseDtoList);
    }

    @PatchMapping
    public ResponseEntity<Void> updateQuantity(
        @AuthenticationPrincipal CustomUserDetails userDetails, // 👈 변경!
        @Valid @RequestBody UpdateCartRequestDto request) {

        cartService.updateQuantity(userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> removeItem(
        @AuthenticationPrincipal CustomUserDetails userDetails, // 👈 변경!
        @PathVariable Long cartItemId) {

        cartService.removeItem(userDetails.getMemberId(), cartItemId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> removeCart(
        @AuthenticationPrincipal CustomUserDetails userDetails) { // 👈 변경!

        cartService.removeCart(userDetails.getMemberId());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
