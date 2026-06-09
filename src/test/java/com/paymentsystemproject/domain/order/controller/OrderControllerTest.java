package com.paymentsystemproject.domain.order.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;

import com.paymentsystemproject.domain.order.dto.CancelOrderResponseDto;
import com.paymentsystemproject.domain.order.dto.CreateOrderRequestDto;
import com.paymentsystemproject.domain.order.dto.CreateOrderResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderDetailResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderPreviewItemDto;
import com.paymentsystemproject.domain.order.dto.GetOrderPreviewResponseDto;
import com.paymentsystemproject.domain.order.entity.OrderStatus;
import com.paymentsystemproject.domain.order.facade.OrderFacade;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;
import com.paymentsystemproject.global.security.CustomUserDetails;
import com.paymentsystemproject.global.security.jwt.JwtAuthenticationEntryPoint;
import com.paymentsystemproject.global.security.jwt.JwtTokenProvider;

@WebMvcTest(OrderController.class)
@TestPropertySource(properties = "encoder.strength=10")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderFacade orderFacade;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private Authentication auth;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = CustomUserDetails.ofToken(1L, "test@test.com");
        auth = new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());
    }

    // ===== 주문서 미리보기 =====

    @Test
    @DisplayName("주문서 미리보기 - 성공 200")
    void getOrderPreview_성공() throws Exception {
        // given
        GetOrderPreviewResponseDto response = new GetOrderPreviewResponseDto(
            List.of(new GetOrderPreviewItemDto(1L, "테스트상품", 10000, 2, 20000)),
            20000, 5000
        );
        given(orderFacade.getOrderPreview(eq(1L), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/orders/preview")
                .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalAmount").value(20000))
            .andExpect(jsonPath("$.data.pointBalance").value(5000))
            .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("주문서 미리보기 - 장바구니 비어있을 때 400")
    void getOrderPreview_빈장바구니_400() throws Exception {
        // given
        given(orderFacade.getOrderPreview(eq(1L), any()))
            .willThrow(new BusinessException(ErrorCode.CART_EMPTY));

        // when & then
        mockMvc.perform(get("/api/orders/preview")
                .with(authentication(auth)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ORDER_002"));
    }

    // ===== 주문 생성 =====

    @Test
    @DisplayName("주문 생성 - 성공 201")
    void createOrder_성공() throws Exception {
        // given
        CreateOrderResponseDto response = new CreateOrderResponseDto(
            1L, "order-uuid", "portone-id", 20000, 0, 20000
        );
        given(orderFacade.createOrder(anyLong(), any())).willReturn(response);

        String body = objectMapper.writeValueAsString(
            new CreateOrderRequestDto(List.of(1L, 2L), 0)
        );

        // when & then
        mockMvc.perform(post("/api/orders")
                .with(authentication(auth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.orderId").value(1L))
            .andExpect(jsonPath("$.data.portonePaymentId").value("portone-id"))
            .andExpect(jsonPath("$.data.pgAmount").value(20000));
    }

    @Test
    @DisplayName("주문 생성 - cartItemIds 비어있으면 400")
    void createOrder_빈_cartItemIds_400() throws Exception {
        // when & then
        mockMvc.perform(post("/api/orders")
                .with(authentication(auth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartItemIds\": [], \"usePoint\": 0}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주문 생성 - usePoint 음수이면 400")
    void createOrder_음수_usePoint_400() throws Exception {
        // when & then
        mockMvc.perform(post("/api/orders")
                .with(authentication(auth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartItemIds\": [1], \"usePoint\": -100}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주문 생성 - 포인트 부족 400")
    void createOrder_포인트부족_400() throws Exception {
        // given
        given(orderFacade.createOrder(anyLong(), any()))
            .willThrow(new BusinessException(ErrorCode.INSUFFICIENT_POINT));

        String body = objectMapper.writeValueAsString(
            new CreateOrderRequestDto(List.of(1L), 99999)
        );

        // when & then
        mockMvc.perform(post("/api/orders")
                .with(authentication(auth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PAYMENT_001"));
    }

    // ===== 내 주문 목록 조회 =====

    @Test
    @DisplayName("내 주문 목록 조회 - 성공 200")
    void getOrderList_성공() throws Exception {
        // given
        given(orderFacade.getOrderList(anyLong(), anyInt(), anyInt()))
            .willReturn(new PageImpl<>(List.of()));

        // when & then
        mockMvc.perform(get("/api/orders")
                .with(authentication(auth))
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isArray());
    }

    // ===== 주문 상세 조회 =====

    @Test
    @DisplayName("주문 상세 조회 - 성공 200")
    void getOrderDetail_성공() throws Exception {
        // given
        GetOrderDetailResponseDto response = new GetOrderDetailResponseDto(
            1L, "order-uuid", OrderStatus.PENDING_PAYMENT, 20000, 200,
            List.of(), null, LocalDateTime.now()
        );
        given(orderFacade.getOrderDetail(1L, 1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/orders/1")
                .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderId").value(1L))
            .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));
    }

    @Test
    @DisplayName("주문 상세 조회 - 없는 주문 404")
    void getOrderDetail_없는주문_404() throws Exception {
        // given
        given(orderFacade.getOrderDetail(1L, 9999L))
            .willThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/orders/9999")
                .with(authentication(auth)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ORDER_003"));
    }

    // ===== 주문 취소 =====

    @Test
    @DisplayName("주문 취소 - 성공 200")
    void cancelOrder_성공() throws Exception {
        // given
        CancelOrderResponseDto response = new CancelOrderResponseDto(1L, OrderStatus.CANCELLED);
        given(orderFacade.cancelOrder(1L, 1L)).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/orders/1/cancel")
                .with(authentication(auth))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderId").value(1L))
            .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("주문 취소 - 결제대기 아닌 상태 400")
    void cancelOrder_잘못된상태_400() throws Exception {
        // given
        given(orderFacade.cancelOrder(1L, 1L))
            .willThrow(new BusinessException(ErrorCode.INVALID_ORDER_STATUS));

        // when & then
        mockMvc.perform(post("/api/orders/1/cancel")
                .with(authentication(auth))
                .with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ORDER_004"));
    }
}
