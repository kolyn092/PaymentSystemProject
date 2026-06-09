package com.paymentsystemproject.domain.order.facade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import com.paymentsystemproject.domain.cartitem.entity.CartItem;
import com.paymentsystemproject.domain.cartitem.service.CartService;
import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.member.service.MemberService;
import com.paymentsystemproject.domain.order.dto.CancelOrderResponseDto;
import com.paymentsystemproject.domain.order.dto.CreateOrderRequestDto;
import com.paymentsystemproject.domain.order.dto.CreateOrderResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderDetailResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderListResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderPreviewResponseDto;
import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.order.entity.OrderStatus;
import com.paymentsystemproject.domain.order.service.OrderService;
import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.domain.payment.service.PaymentService;
import com.paymentsystemproject.domain.product.entity.Product;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderFacadeTest {

    @InjectMocks
    private OrderFacade orderFacade;

    @Mock private MemberService memberService;
    @Mock private CartService cartService;
    @Mock private OrderService orderService;
    @Mock private PaymentService paymentService;

    private Member member;
    private CartItem cartItem1;
    private CartItem cartItem2;
    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        member = new Member("test@test.com", "password", "테스트유저", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", 1L);
        ReflectionTestUtils.setField(member, "pointBalance", 10000);

        Product product1 = mock(Product.class);
        given(product1.getId()).willReturn(1L);
        given(product1.getName()).willReturn("테스트상품A");
        given(product1.getPrice()).willReturn(10000);

        Product product2 = mock(Product.class);
        given(product2.getId()).willReturn(2L);
        given(product2.getName()).willReturn("테스트상품B");
        given(product2.getPrice()).willReturn(20000);

        cartItem1 = mock(CartItem.class);
        given(cartItem1.getProduct()).willReturn(product1);
        given(cartItem1.getQuantity()).willReturn(2);

        cartItem2 = mock(CartItem.class);
        given(cartItem2.getProduct()).willReturn(product2);
        given(cartItem2.getQuantity()).willReturn(1);

        order = mock(Order.class);
        given(order.getId()).willReturn(1L);
        given(order.getOrderNumber()).willReturn(UUID.randomUUID().toString());
        given(order.getTotalAmount()).willReturn(40000);
        given(order.getStatus()).willReturn(OrderStatus.PENDING_PAYMENT);
        given(order.getOrderItems()).willReturn(List.of());

        payment = mock(Payment.class);
        given(payment.getPortonePaymentId()).willReturn("portone-test-id");
        given(payment.getUsePoint()).willReturn(0);
        given(payment.getPgAmount()).willReturn(40000);
        given(payment.getEarnedPoint()).willReturn(400);
    }

    // ===== 주문서 미리보기 =====

    @Test
    @DisplayName("주문서 미리보기 - 전체 장바구니 조회 성공")
    void getOrderPreview_전체조회_성공() {
        // given
        given(memberService.findById(1L)).willReturn(member);
        given(cartService.findCartEntities(1L)).willReturn(List.of(cartItem1, cartItem2));

        // when
        GetOrderPreviewResponseDto result = orderFacade.getOrderPreview(1L, null);

        // then
        assertThat(result.items()).hasSize(2);
        assertThat(result.totalAmount()).isEqualTo(40000);
        assertThat(result.pointBalance()).isEqualTo(10000);
    }

    @Test
    @DisplayName("주문서 미리보기 - 선택 항목 조회 성공")
    void getOrderPreview_선택조회_성공() {
        // given
        given(memberService.findById(1L)).willReturn(member);
        given(cartService.findCartEntitiesById(1L, List.of(1L))).willReturn(List.of(cartItem1));

        // when
        GetOrderPreviewResponseDto result = orderFacade.getOrderPreview(1L, List.of(1L));

        // then
        assertThat(result.items()).hasSize(1);
        assertThat(result.totalAmount()).isEqualTo(20000);
    }

    @Test
    @DisplayName("주문서 미리보기 - 장바구니 비어있으면 실패")
    void getOrderPreview_빈장바구니_실패() {
        // given
        given(memberService.findById(1L)).willReturn(member);
        given(cartService.findCartEntities(1L)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> orderFacade.getOrderPreview(1L, null))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_EMPTY);
    }

    @Test
    @DisplayName("주문서 미리보기 - 존재하지 않는 cartItemId 실패")
    void getOrderPreview_잘못된cartItemId_실패() {
        // given
        given(memberService.findById(1L)).willReturn(member);
        given(cartService.findCartEntitiesById(1L, List.of(9999L))).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> orderFacade.getOrderPreview(1L, List.of(9999L)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_EMPTY);
    }

    // ===== 주문 생성 =====

    @Test
    @DisplayName("주문 생성 - 성공")
    void createOrder_성공() {
        // given
        given(memberService.findById(1L)).willReturn(member);
        given(cartService.findCartEntitiesById(1L, List.of(1L, 2L))).willReturn(List.of(cartItem1, cartItem2));
        given(orderService.createOrder(any(), any(), any())).willReturn(order);
        given(paymentService.createPayment(any(), any(), any())).willReturn(payment);

        // when
        CreateOrderResponseDto result = orderFacade.createOrder(1L, new CreateOrderRequestDto(List.of(1L, 2L), 0));

        // then
        assertThat(result.orderId()).isEqualTo(1L);
        assertThat(result.portonePaymentId()).isEqualTo("portone-test-id");
    }

    @Test
    @DisplayName("주문 생성 - 장바구니 비어있으면 실패")
    void createOrder_빈장바구니_실패() {
        // given
        given(memberService.findById(1L)).willReturn(member);
        given(cartService.findCartEntitiesById(1L, List.of(9999L))).willReturn(List.of());

        // when & then
        assertThatThrownBy(() ->
            orderFacade.createOrder(1L, new CreateOrderRequestDto(List.of(9999L), 0)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_EMPTY);
    }

    // ===== 내 주문 목록 조회 =====

    @Test
    @DisplayName("내 주문 목록 조회 - 성공")
    void getOrderList_성공() {
        // given
        given(memberService.findById(1L)).willReturn(member);
        given(orderService.getOrderList(any(), anyInt(), anyInt()))
            .willReturn(new PageImpl<>(List.of()));

        // when
        var result = orderFacade.getOrderList(1L, 0, 20);

        // then
        assertThat(result).isNotNull();
    }

    // ===== 주문 상세 조회 =====

    @Test
    @DisplayName("주문 상세 조회 - 성공")
    void getOrderDetail_성공() {
        // given
        given(orderService.getOrderDetail(1L, 1L)).willReturn(order);
        given(paymentService.findByOrderIdWithOrder(1L)).willReturn(payment);

        // when
        GetOrderDetailResponseDto result = orderFacade.getOrderDetail(1L, 1L);

        // then
        assertThat(result.orderId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("주문 상세 조회 - 존재하지 않는 주문 실패")
    void getOrderDetail_없는주문_실패() {
        // given
        given(orderService.getOrderDetail(1L, 9999L))
            .willThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> orderFacade.getOrderDetail(1L, 9999L))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    // ===== 주문 취소 =====

    @Test
    @DisplayName("주문 취소 - 성공")
    void cancelOrder_성공() {
        // given
        given(orderService.cancelOrder(1L, 1L)).willReturn(order);
        given(paymentService.findByOrderIdWithOrder(1L)).willReturn(payment);

        // when
        CancelOrderResponseDto result = orderFacade.cancelOrder(1L, 1L);

        // then
        assertThat(result.orderId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("주문 취소 - 결제대기 아닌 상태 실패")
    void cancelOrder_잘못된상태_실패() {
        // given
        given(orderService.cancelOrder(1L, 1L))
            .willThrow(new BusinessException(ErrorCode.INVALID_ORDER_STATUS));

        // when & then
        assertThatThrownBy(() -> orderFacade.cancelOrder(1L, 1L))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    @DisplayName("주문 취소 - 존재하지 않는 주문 실패")
    void cancelOrder_없는주문_실패() {
        // given
        given(orderService.cancelOrder(1L, 9999L))
            .willThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> orderFacade.cancelOrder(1L, 9999L))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }
}
