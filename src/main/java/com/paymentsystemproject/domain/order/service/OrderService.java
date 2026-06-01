package com.paymentsystemproject.domain.order.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.cartitem.entity.CartItem;
import com.paymentsystemproject.domain.cartitem.repository.CartItemRepository;
import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.member.repository.MemberRepository;
import com.paymentsystemproject.domain.order.dto.CreateOrderRequestDto;
import com.paymentsystemproject.domain.order.dto.CreateOrderResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderListResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderPreviewItemDto;
import com.paymentsystemproject.domain.order.dto.GetOrderPreviewResponseDto;
import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.order.repository.OrderRepository;
import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.domain.payment.service.PaymentService;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentService paymentService;

    @Transactional(readOnly = true)
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

    @Transactional
    public CreateOrderResponseDto createOrder(Long memberId, CreateOrderRequestDto requestDto) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // cartItemIds 선택 조회 메서드명 확인 후 수정
        List<CartItem> cartItems = cartItemRepository.findByIdAndMemberId(requestDto.cartItemIds(), memberId);
        if (cartItems.size() != requestDto.cartItemIds().size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        Integer totalAmount = cartItems.stream()
            .mapToInt(cartItem -> cartItem.getProduct().getPrice() * cartItem.getQuantity())
            .sum();

        String orderNumber = UUID.randomUUID().toString();

        Order order = new Order(member, orderNumber, totalAmount);
        orderRepository.save(order);

        // 재고 선차감 로직 추가 필요 (Product 재고 차감)

        Payment payment = paymentService.createPayment(order, requestDto.usePoint(), member.getPointBalance());

        // PortOne 결제창 호출 로직 추가 필요

        return CreateOrderResponseDto.from(order, payment);
    }

    @Transactional(readOnly = true)
    public Page<GetOrderListResponseDto> getOrderList(Long memberId, int page, int size) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return orderRepository.findByMember(member, pageable)
            .map(GetOrderListResponseDto::from);
    }
}
