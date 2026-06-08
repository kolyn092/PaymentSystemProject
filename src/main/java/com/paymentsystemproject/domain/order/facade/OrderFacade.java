package com.paymentsystemproject.domain.order.facade;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.cartitem.entity.CartItem;
import com.paymentsystemproject.domain.cartitem.service.CartService;
import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.member.service.MemberService;
import com.paymentsystemproject.domain.order.dto.CancelOrderResponseDto;
import com.paymentsystemproject.domain.order.dto.CreateOrderRequestDto;
import com.paymentsystemproject.domain.order.dto.CreateOrderResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderDetailResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderListResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderPreviewItemDto;
import com.paymentsystemproject.domain.order.dto.GetOrderPreviewResponseDto;
import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.order.entity.OrderItem;
import com.paymentsystemproject.domain.order.service.OrderService;
import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.domain.payment.entity.PaymentStatus;
import com.paymentsystemproject.domain.payment.service.PaymentService;
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

    private final MemberService memberService;
    private final CartService cartService;
    private final OrderService orderService;
    private final PaymentService paymentService;

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
     * 장바구니에서 선택한 상품들로 주문을 생성하고 결제 정보를 초기화합니다.
     * 재고를 먼저 선차감한 후 주문 항목을 생성하며, 결제 정보도 함께 초기화합니다.
     *
     * @param memberId 회원 ID
     * @param requestDto 주문할 장바구니 항목 ID 목록 및 사용할 포인트
     * @return 생성된 주문 정보 및 결제 준비 정보
     */

    public CreateOrderResponseDto createOrder(Long memberId, CreateOrderRequestDto requestDto) {
        Member member = memberService.findById(memberId);

        List<CartItem> cartItems = getValidCartItems(memberId, requestDto.cartItemIds());

        for (CartItem cartItem : cartItems) {
            cartItem.getProduct().decreaseStock(cartItem.getQuantity());
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            orderItems.add(new OrderItem(
                cartItem.getProduct(),
                cartItem.getProduct().getName(),
                cartItem.getProduct().getPrice(),
                cartItem.getQuantity()
            ));
        }

        int totalAmount = cartItems.stream()
            .mapToInt(cartItem -> cartItem.getProduct().getPrice() * cartItem.getQuantity())
            .sum();

        Order order = orderService.createOrder(member, orderItems, totalAmount);
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
        Member member = memberService.findById(memberId);
        return orderService.getOrderList(member, page, size);
    }

    /**
     * 특정 주문의 상세 정보를 조회합니다.
     * orderId와 memberId를 함께 조회하여 본인 주문만 접근할 수 있도록 보장합니다.
     *
     * @param memberId 회원 ID
     * @param orderId 조회할 주문 ID
     * @return 주문 상세 정보 및 결제 정보
     */

    @Transactional(readOnly = true)
    public GetOrderDetailResponseDto getOrderDetail(Long memberId, Long orderId) {
        Order order = orderService.getOrderDetail(memberId, orderId);
        Payment payment = paymentService.findByOrderIdWithOrder(orderId);

        return GetOrderDetailResponseDto.from(order, payment);
    }

    /**
     * 결제 대기 상태인 주문을 취소합니다.
     * 주문 취소 후 결제 상태를 실패로 변경합니다.
     *
     * @param memberId 회원 ID
     * @param orderId 취소할 주문 ID
     * @return 취소된 주문 정보
     */

    public CancelOrderResponseDto cancelOrder(Long memberId, Long orderId) {
        Order order = orderService.cancelOrder(memberId, orderId);
        Payment payment = paymentService.findByOrderIdWithOrder(orderId);
        payment.changeStatus(PaymentStatus.FAILED);

        return CancelOrderResponseDto.from(order);
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
