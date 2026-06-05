package com.paymentsystemproject.domain.order.facade;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.cartitem.entity.CartItem;
import com.paymentsystemproject.domain.cartitem.service.CartService;
import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.member.service.MemberService;
import com.paymentsystemproject.domain.order.dto.GetOrderPreviewItemDto;
import com.paymentsystemproject.domain.order.dto.GetOrderPreviewResponseDto;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 주문 관련 Facade 클래스입니다.
 * 여러 도메인(회원, 장바구니, 결제)에 걸친 흐름을 조율합니다.
 */

@Component
@RequiredArgsConstructor
@Transactional
public class OrderFacade {

    private MemberService memberService;
    private CartService cartService;

    /**
     * 장바구니에 담긴 상품들을 결제 직전의 '주문서' 형태로 미리보기 위한 기능입니다.
     * 장바구니 상품 ID 목록이 없으면 전체 장바구니 항목을 조회하며, 실시간 가격이 반영됩니다.
     *
     * @param memberId 회원 ID
     * @param cartItemIds 결제할 장바구니 항목 ID 목록 (선택)
     * @return 결제 화면 구성에 필요한 주문서 정보 (총액, 상품 목록, 사용 가능 포인트)
     */

    public GetOrderPreviewResponseDto getOrderPreview(Long memberId, List<Long> cartItemIds) {
        Member member = memberService.findById(memberId);

        List<CartItem> cartItems = getValidCartItems(memberId, cartItemIds);

        List<GetOrderPreviewItemDto> items = cartItems.stream()
            .map(GetOrderPreviewItemDto::from)
            .toList();

        int totalAmount = items.stream()
            .mapToInt(GetOrderPreviewItemDto::subtotal)
            .sum();

        return GetOrderPreviewResponseDto.of(items, totalAmount, member.getPointBalance());
    }

    /**
     * 장바구니 항목을 조회하고 유효성 검사를 합니다.
     * ID 목록이 없으면 전체 장바구니를, 있으면 해당 항목만 조회합니다.
     *
     * @param memberId 회원 ID
     * @param cartItemIds 조회할 장바구니 항목 ID 목록 (null 또는 빈 리스트면 전체 조회)
     * @return 유효한 자아바구니 항목 조회
     */

    private List<CartItem> getValidCartItems(Long memberId, List<Long> cartItemIds) {
        List<CartItem> cartItems = (cartItemIds == null || cartItemIds.isEmpty())
            ? cartService.findCartEntities(memberId)
            : cartService.findCartEntitiesById(memberId, cartItemIds);

        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        if (cartItemIds != null && !cartItemIds.isEmpty() && cartItems.size() != cartItemIds.size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        return cartItems;
    }
}
