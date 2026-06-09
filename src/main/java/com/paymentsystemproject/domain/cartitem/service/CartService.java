package com.paymentsystemproject.domain.cartitem.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.cartitem.dto.AddCartRequestDto;
import com.paymentsystemproject.domain.cartitem.dto.AddCartResponseDto;
import com.paymentsystemproject.domain.cartitem.dto.GetCartItemResponseDto;
import com.paymentsystemproject.domain.cartitem.dto.GetCartResponseDto;
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
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public AddCartResponseDto addItem(Long memberId, AddCartRequestDto requestDto) {
        Optional<CartItem> existing = cartItemRepository.findByMember_idAndProduct_Id(memberId, requestDto.productId());

        if (existing.isPresent()) {
            CartItem addItem = existing.get();
            addItem.addQuantity(requestDto.quantity());
            return AddCartResponseDto.from(addItem);
        } else {
            Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
            );
            Product product = productRepository.findById(requestDto.productId()).orElseThrow(
                () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
            );

            CartItem cartItem = CartItem.from(member, product, requestDto.quantity());
            CartItem savedItem = cartItemRepository.save(cartItem);
            return AddCartResponseDto.from(savedItem);
        }
    }

    @Transactional(readOnly = true)
    public GetCartResponseDto getCartItems(Long memberId) {
        List<GetCartItemResponseDto> items = cartItemRepository.findByMemberId(memberId).stream()
            .map(GetCartItemResponseDto::from)
            .toList();

        int totalPrice = items.stream().mapToInt(item -> item.price() * item.quantity()).sum();

        return new GetCartResponseDto(items, totalPrice);
    }

    @Transactional(readOnly = true)
    public GetCartResponseDto getSelectedItems(Long memberId, List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return new GetCartResponseDto(List.of(), 0);
        }

        List<CartItem> items = cartItemRepository.findByMemberIdAndIdIn(memberId, cartItemIds);

        if (items.size() != cartItemIds.size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        List<GetCartItemResponseDto> cartItems = items.stream()
            .map(GetCartItemResponseDto::from)
            .toList();

        int totalPrice = cartItems.stream().mapToInt(item -> item.price() * item.quantity()).sum();

        return new GetCartResponseDto(cartItems, totalPrice);
    }

    @Transactional
    public void updateQuantity(Long memberId, UpdateCartRequestDto requestDto) {
        CartItem item = cartItemRepository.findByMember_idAndProduct_Id(memberId, requestDto.productId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        item.changeQuantity(requestDto.quantity());
    }

    @Transactional
    public void removeItem(Long memberId, Long itemId) {
        int item = cartItemRepository.deleteByIdAndMember_Id(itemId, memberId);
        if (item == 0) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
    }

    @Transactional
    public void removeCart(Long memberId) {
        int item = cartItemRepository.deleteAllByMemberId(memberId);
        if (item == 0) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
    }

    /**
     * 회원의 전체 장바구니 항목을 엔티티로 조회
     * Facade에서 주문 처리 시 엔티티 필요한 경우 사용
     *
     * @param memberId 회원 ID
     * @return 장바구니 항목 엔티티 목록
     */

    public List<CartItem> findCartEntities(Long memberId) {
        return cartItemRepository.findByMemberId(memberId);
    }

    /**
     * 회원의 장바구니 항목 중 지정된 ID 목록에 해당하는 항목을 엔티티로 조회
     * Facade에서 주문 처리 시 엔티티가 필요한 경우 사용
     *
     * @param memberId 회원 ID
     * @param cartItemIds 조회할 장바구니 항목 ID 목록
     * @return 장바구니 항목 엔티티 목록
     */

    public List<CartItem> findCartEntitiesById(Long memberId, List<Long> cartItemIds) {
        return cartItemRepository.findByMemberIdAndIdIn(memberId, cartItemIds);
    }
}



