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

    @Transactional
    public Long addItem(AddCartRequestDto requestDto) {
        Optional<CartItem> existing = cartItemRepository.findByMember_idAndProduct_Id(requestDto.memberId(),
            requestDto.productId());

        if (existing.isPresent()) {
            CartItem addItem = existing.get();
            addItem.addQuantity(requestDto.quantity());
            return addItem.getId();
            //else 문 추후 구현 예정
        } else {
            //
            //     Member member = memberRepository.findById(memberId);
            Product product = productRepository.findById(requestDto.productId()).orElseThrow(
                () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
            );

            //     CartItem cartItem = CartItem.from(member, product, requestDto.getQuantity);
            //
            //     return cartItemRepository.save(cartItem).getId();
            // }
        }
        return null;
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

// public List<CartItemResponse> getCartItems(Long memberId) {
//     return cartItemRepository.findByMemberId(memberId).stream()
//         .map(this::toResponse)
//         .toList();
// }
//
// @Transactional
// public Long addItem(CartItem cartItem) {
//     Optional<CartItem> existing = cartItemRepository.findByMember_IdAndProduct_Id(
//         cartItem.getMemberId(), cartItem.getProductId()
//     );
//     if (existing.isPresent()) {
//         CartItem found = existing.get();
//         found.addQuantity(cartItem.getQuantity());
//         return found.getId();
//     } else {
//         return cartItemRepository.save(cartItem).getId();
//     }
// }
//
// @Transactional
// public void updateQuantity(Long memberId, Long itemId, int quantity) {
//     CartItem item = cartItemRepository.findById(itemId)
//         .filter(ci -> ci.getMemberId().equals(memberId))
//         .orElseThrow(() -> new RuntimeException("장바구니 항목을 찾을 수 없습니다."));
//     item.changeQuantity(quantity);
// }
//
// @Transactional
// public void removeItem(Long memberId, Long itemId) {
//     int deleted = cartItemRepository.deleteByIdAndMember_Id(itemId, memberId);
//     if (deleted == 0) {
//         throw new RuntimeException("장바구니 항목을 찾을 수 없습니다.");
//     }
// }
//
// private CartItemResponse toResponse(CartItem item) {
//     return new CartItemResponse(
//         item.getId(),
//         item.getProduct().getId(),
//         item.getProduct().getName(),
//         item.getProduct().getPrice(),
//         item.getQuantity(),
//         item.getProduct().getStock()
//     );
// }

