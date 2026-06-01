package com.paymentsystemproject.domain.order.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.paymentsystemproject.domain.cartitem.entity.CartItem;
import com.paymentsystemproject.domain.cartitem.repository.CartItemRepository;
import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.member.repository.MemberRepository;
import com.paymentsystemproject.domain.order.dto.GetOrderPreviewItemDto;
import com.paymentsystemproject.domain.order.dto.GetOrderPreviewResponseDto;
import com.paymentsystemproject.domain.order.repository.OrderRepository;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final CartItemRepository cartItemRepository;

    public GetOrderPreviewResponseDto getOrderPreview(Long memberId, List<Long> cartItemIds) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<CartItem> cartItems;
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            cartItems = cartItemRepository.findByMemberId(memberId);
        } else {
            // cartItemIds 선택 조회 메서드명 확인 후 수정
            cartItems = cartItemRepository.findByIdAndMemberId(cartItemIds, memberId);
            if (cartItems.size() != cartItemIds.size()) {
                throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
            }
        }

        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        List<GetOrderPreviewItemDto> items = cartItems.stream()
            .map(GetOrderPreviewItemDto::from)
            .toList();

        int totalAmount = items.stream()
            .mapToInt(GetOrderPreviewItemDto::subtotal)
            .sum();

        return GetOrderPreviewResponseDto.of(items, totalAmount, member.getPointBalance());
    }
}
