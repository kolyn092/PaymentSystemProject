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
import com.paymentsystemproject.domain.order.dto.CancelOrderResponseDto;
import com.paymentsystemproject.domain.order.dto.CreateOrderRequestDto;
import com.paymentsystemproject.domain.order.dto.CreateOrderResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderDetailResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderListResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderPreviewItemDto;
import com.paymentsystemproject.domain.order.dto.GetOrderPreviewResponseDto;
import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.order.entity.OrderItem;
import com.paymentsystemproject.domain.order.entity.OrderStatus;
import com.paymentsystemproject.domain.order.repository.OrderItemRepository;
import com.paymentsystemproject.domain.order.repository.OrderRepository;
import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.domain.payment.entity.PaymentStatus;
import com.paymentsystemproject.domain.payment.repository.PaymentRepository;
import com.paymentsystemproject.domain.payment.service.PaymentService;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 주문 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 주문서 미리보기, 주문 생성(결제 준비), 주문 내역 조회 및 취소 등의 기능을 제공합니다.
 */

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberRepository memberRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    /**
     * 장바구니에 담긴 상품들을 결제 직전의 '주문서' 형태로 미리보기 위한 기능입니다.
     * 장바구니 상품 ID 목록이 없으면 전체 장바구니 항목을 조회하며, 실시간 가격이 반영됩니다.
     *
     * @param memberId 회원 ID
     * @param cartItemIds 결제할 장바구니 항목 ID 목록 (선택)
     * @return 결제 화면 구성에 필요한 주문서 정보 (총액, 상품 목록 등)
     */

    @Transactional(readOnly = true)
    public GetOrderPreviewResponseDto getOrderPreview(Long memberId, List<Long> cartItemIds) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<CartItem> cartItems;
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            cartItems = cartItemRepository.findByMemberId(memberId);
        } else {
            cartItems = cartItemRepository.findByMemberIdAndIdIn(memberId, cartItemIds);
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

    /**
     * 장바구니에서 선택한 상품들로 주문을 생성하고 결제 정보를 초기화합니다.
     * 주문 번호는 UUID로 생성되며, 주문 항목은 장바구니 상품 정보를 기반으로 저장됩니다.
     * 결제 완료 전 재고를 선차감하여 동시 주문으로 인한 초과 판매를 방지합니다.
     *
     * @param memberId 회원 ID
     * @param requestDto 주문할 장바구니 항목 ID 목록 및 사용할 포인트
     * @return 생성된 주문 정보 및 결제 준비 정보
     */

    @Transactional
    public CreateOrderResponseDto createOrder(Long memberId, CreateOrderRequestDto requestDto) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<CartItem> cartItems = cartItemRepository.findByMemberIdAndIdIn(memberId, requestDto.cartItemIds());
        if (cartItems.size() != requestDto.cartItemIds().size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        Integer totalAmount = cartItems.stream()
            .mapToInt(cartItem -> cartItem.getProduct().getPrice() * cartItem.getQuantity())
            .sum();

        String orderNumber = UUID.randomUUID().toString();

        Order order = new Order(member, orderNumber, totalAmount);
        orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem(
                order,
                cartItem.getProduct(),
                cartItem.getProduct().getName(),
                cartItem.getProduct().getPrice(),
                cartItem.getQuantity()
            );
            orderItemRepository.save(orderItem);
        }

        for (CartItem cartItem : cartItems) {
            cartItem.getProduct().decreaseStock(cartItem.getQuantity());
        }

        Payment payment = paymentService.createPayment(order, requestDto.usePoint(), member.getPointBalance());

        return CreateOrderResponseDto.from(order, payment);
    }

    /**
     * 로그인한 회원의 주문 목록을 최신순으로 페이징하여 반환합니다.
     *
     * @param memberId 회원 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 항목 수
     * @return 주문 목록 (페이징)
     */

    @Transactional(readOnly = true)
    public Page<GetOrderListResponseDto> getOrderList(Long memberId, int page, int size) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return orderRepository.findByMember(member, pageable)
            .map(GetOrderListResponseDto::from);
    }

    /**
     * 특정 주문의 상세 정보를 조회합니다.
     * orderId와 memberId를 함께 조회하여 본인 주문만 접근할 수 있도록 보장하며,
     * JOIN FETCH를 사용해 함께 주문 항목을 한 번에 가져와 N+1 문제를 방지합니다.
     *
     * @param memberId 회원 ID
     * @param orderId 주문 ID
     * @return 주문 상세 정보 및 결제 정보
     */

    @Transactional(readOnly = true)
    public GetOrderDetailResponseDto getOrderDetail(Long memberId, Long orderId) {
        Order order = orderRepository.findByIdAndMemberIdWithOrderItems(orderId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        Payment payment = paymentRepository.findByOrder(order)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        return GetOrderDetailResponseDto.from(order, payment);
    }

    /**
     * 결제 대기 상태인 주문을 취소합니다.
     * orderId와 memberId를 함께 조회하여 본인 부문만 취소할 수 있도록 보장합니다.
     * 취소 시 선차감했던 재고를 복구하고, 결제 상태를 FAILED로 변경합니다.
     *
     * @param memberId 회원 ID
     * @param orderId 취소할 주문 ID
     * @return 취소된 주문 정보
     */

    @Transactional
    public CancelOrderResponseDto cancelOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findByIdAndMemberIdWithOrderItems(orderId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Payment payment = paymentRepository.findByOrder(order)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        for (OrderItem orderItem : order.getOrderItems()) {
            orderItem.getProduct().restoreStock(orderItem.getQuantity());
        }
        payment.changeStatus(PaymentStatus.FAILED);

        order.cancel();

        return CancelOrderResponseDto.from(order);
    }
}
