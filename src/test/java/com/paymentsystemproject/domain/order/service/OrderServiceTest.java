package com.paymentsystemproject.domain.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
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

import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.order.entity.OrderStatus;
import com.paymentsystemproject.domain.order.repository.OrderRepository;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    private Member member;
    private Order order;

    @BeforeEach
    void setUp() {
        member = new Member("test@test.com", "password", "테스트유저", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", 1L);

        order = mock(Order.class);
        given(order.getId()).willReturn(1L);
        given(order.getOrderNumber()).willReturn(UUID.randomUUID().toString());
        given(order.getStatus()).willReturn(OrderStatus.PENDING_PAYMENT);
        given(order.getTotalAmount()).willReturn(10000);
        given(order.getOrderItems()).willReturn(List.of());
    }

    // ===== 주문 생성 =====

    @Test
    @DisplayName("주문 생성 - 성공")
    void createOrder_성공() {
        // given
        given(orderRepository.save(any())).willReturn(order);

        // when & then
        assertThatNoException().isThrownBy(() ->
            orderService.createOrder(member, List.of(), 10000));
    }

    // ===== 내 주문 목록 조회 =====

    @Test
    @DisplayName("내 주문 목록 조회 - 성공")
    void getOrderList_성공() {
        // given
        given(orderRepository.findByMember(any(), any())).willReturn(new PageImpl<>(List.of(order)));

        // when
        var result = orderService.getOrderList(member, 0, 20);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("내 주문 목록 조회 - 빈 목록 성공")
    void getOrderList_빈목록_성공() {
        // given
        given(orderRepository.findByMember(any(), any())).willReturn(new PageImpl<>(List.of()));

        // when
        var result = orderService.getOrderList(member, 0, 20);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    // ===== 주문 상세 조회 =====

    @Test
    @DisplayName("주문 상세 조회 - 성공")
    void getOrderDetail_성공() {
        // given
        given(orderRepository.findByIdAndMemberIdWithOrderItems(1L, 1L)).willReturn(Optional.of(order));

        // when
        Order result = orderService.getOrderDetail(1L, 1L);

        // then
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("주문 상세 조회 - 존재하지 않는 주문 실패")
    void getOrderDetail_없는주문_실패() {
        // given
        given(orderRepository.findByIdAndMemberIdWithOrderItems(1L, 9999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.getOrderDetail(1L, 9999L))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    // ===== 주문 취소 =====

    @Test
    @DisplayName("주문 취소 - 성공")
    void cancelOrder_성공() {
        // given
        given(orderRepository.findByIdAndMemberIdWithOrderItems(1L, 1L)).willReturn(Optional.of(order));

        // when & then
        assertThatNoException().isThrownBy(() -> orderService.cancelOrder(1L, 1L));
    }

    @Test
    @DisplayName("주문 취소 - 결제대기 아닌 상태 실패")
    void cancelOrder_잘못된상태_실패() {
        // given
        Order completedOrder = new Order(member, UUID.randomUUID().toString(), 10000, List.of());
        ReflectionTestUtils.setField(completedOrder, "status", OrderStatus.COMPLETED);
        given(orderRepository.findByIdAndMemberIdWithOrderItems(1L, 1L)).willReturn(Optional.of(completedOrder));

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    @DisplayName("주문 취소 - 존재하지 않는 주문 실패")
    void cancelOrder_없는주문_실패() {
        // given
        given(orderRepository.findByIdAndMemberIdWithOrderItems(1L, 9999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, 9999L))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }
}
