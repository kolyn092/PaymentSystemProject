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
import com.paymentsystemproject.global.security.jwt.JwtTokenProvider;

import jakarta.annotation.PostConstruct;
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
    public ResponseEntity<List<GetCartItemResponseDto>> getItems(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.status(HttpStatus.OK).body(cartService.getCartItems(memberId));
    }

    @GetMapping("/selected")
    public ResponseEntity<List<GetCartItemResponseDto>> getSelectedItems(
        @AuthenticationPrincipal Long memberId,
        @RequestParam(name = "ids", required = false) List<Long> cartItemIds) {

        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(List.of());
        }

        List<GetCartItemResponseDto> responseDtoList = cartService.getSelectedItems(memberId, cartItemIds);
        return ResponseEntity.status(HttpStatus.OK).body(responseDtoList);
    }

    @PatchMapping
    public ResponseEntity<Void> updateQuantity(@AuthenticationPrincipal Long memberId,
        @Valid @RequestBody UpdateCartRequestDto request) {
        cartService.updateQuantity(memberId, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> removeItem(@AuthenticationPrincipal Long memberId, @PathVariable Long cartItemId) {
        cartService.removeItem(memberId, cartItemId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> removeCart(@AuthenticationPrincipal Long memberId) {
        cartService.removeCart(memberId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/test-token")
    public ResponseEntity<String> getTestToken() {
        // 1번 멤버, 테스트 이메일로 유효한 토큰을 강제 발급합니다.
        String token = jwtTokenProvider.createToken(1L, "test@test.com");
        return ResponseEntity.ok(token);
    }

    @PostConstruct
    public void printTestToken() {
        // 서버가 켜질 때 1번 멤버의 토큰을 만들어서 인텔리제이 콘솔창에 강제로 출력합니다.
        String token = jwtTokenProvider.createToken(1L, "test@test.com");
        System.out.println("==================================================");
        System.out.println("🔑 테스트용 임시 토큰 (복사해서 Postman에 쓰세요):");
        System.out.println(token);
        System.out.println("==================================================");
    }
}
