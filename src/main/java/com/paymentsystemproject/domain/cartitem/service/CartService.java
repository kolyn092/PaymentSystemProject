package com.paymentsystemproject.domain.cartitem.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.cartitem.dto.AddCartRequestDto;
import com.paymentsystemproject.domain.cartitem.dto.GetCartItemResponseDto;
import com.paymentsystemproject.domain.cartitem.dto.UpdateCartRequestDto;
import com.paymentsystemproject.domain.cartitem.entity.CartItem;
import com.paymentsystemproject.domain.cartitem.repository.CartItemRepository;
import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.member.repository.MemberRepository;
import com.paymentsystemproject.domain.product.entity.Product;
import com.paymentsystemproject.domain.product.repository.ProductRepository;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long addItem(AddCartRequestDto requestDto) {
        Optional<CartItem> existing = cartItemRepository.findByMember_idAndProduct_Id(requestDto.memberId(),
            requestDto.productId());

        if (existing.isPresent()) {
            CartItem addItem = existing.get();
            addItem.addQuantity(requestDto.quantity());
            return addItem.getId();
        } else {
            Member member = memberRepository.findById(requestDto.memberId()).orElseThrow(
                () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
            );
            Product product = productRepository.findById(requestDto.productId()).orElseThrow(
                () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
            );

            CartItem cartItem = CartItem.from(member, product, requestDto.quantity());
            return cartItemRepository.save(cartItem).getId();
        }
    }

    @Transactional(readOnly = true)
    public List<GetCartItemResponseDto> getCartItems(Long memberId) {
        return cartItemRepository.findByMemberId(memberId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<GetCartItemResponseDto> getSelectedItems(Long memberId, List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return List.of();
        }

        List<CartItem> items = cartItemRepository.findByMemberIdAndIdIn(memberId, cartItemIds);

        if (items.size() != cartItemIds.size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        return items.stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public void updateQuantity(Long memberId, UpdateCartRequestDto requestDto) {
        CartItem item = cartItemRepository.findByMember_idAndProduct_Id(memberId, requestDto.productId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        item.changeQuantity(requestDto.quantity());
    }

    @Transactional
    public void removeItem(Long memberId, Long itemId) {
        cartItemRepository.deleteByIdAndMember_Id(itemId, memberId);
    }

    @Transactional
    public void removeCart(Long memberId) {
        int item = cartItemRepository.deleteAllByMemberId(memberId);
        if (item == 0) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
    }

    private GetCartItemResponseDto toResponse(CartItem item) {
        return new GetCartItemResponseDto(
            item.getId(),
            item.getProduct().getId(),
            item.getProduct().getName(),
            item.getProduct().getPrice(),
            item.getQuantity(),
            item.getProduct().getStock()
        );
    }

}



