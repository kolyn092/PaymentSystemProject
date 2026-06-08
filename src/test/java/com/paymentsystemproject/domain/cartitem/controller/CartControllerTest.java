package com.paymentsystemproject.domain.cartitem.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.paymentsystemproject.domain.cartitem.dto.AddCartRequestDto;
import com.paymentsystemproject.domain.cartitem.dto.GetCartItemResponseDto;
import com.paymentsystemproject.domain.cartitem.dto.GetCartResponseDto;
import com.paymentsystemproject.domain.cartitem.dto.UpdateCartRequestDto;
import com.paymentsystemproject.domain.cartitem.service.CartService;
import com.paymentsystemproject.global.security.CustomUserDetails;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    private CustomUserDetails mockUser;

    @BeforeEach
    void setUp() {
        mockUser = mock(CustomUserDetails.class);
        given(mockUser.getMemberId()).willReturn(1L);
    }

    @Test
    @DisplayName("장바구니 상품 추가 API가 정상적으로 응답한다.")
    void addItem_success() throws Exception {
        AddCartRequestDto request = new AddCartRequestDto(100L, 2);
        given(cartService.addItem(eq(1L), any(AddCartRequestDto.class))).willReturn(10L);

        mockMvc.perform(post("/api/cartitems")
                .with(user(mockUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isCreated()) // 201 Created 검증
            .andExpect(jsonPath("$.data.cartItemId").value(10L));
    }

    @Test
    @DisplayName("장바구니 상품 추가 시 수량이 1 미만이면 400 에러를 반환한다. (@Valid 테스트)")
    void addItem_fail_invalidQuantity() throws Exception {
        AddCartRequestDto invalidRequest = new AddCartRequestDto(100L, 0);

        mockMvc.perform(post("/api/cartitems")
                .with(user(mockUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("본인의 장바구니 목록 조회 API가 정상적으로 응답한다.")
    void getItems_success() throws Exception {
        GetCartItemResponseDto item = new GetCartItemResponseDto(10L, 100L, "마우스", 10000, 2, 50);
        GetCartResponseDto mockResponse = new GetCartResponseDto(List.of(item), 20000);

        given(cartService.getCartItems(1L)).willReturn(mockResponse);

        mockMvc.perform(get("/api/cartitems")
                .with(user(mockUser)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalPrice").value(20000))
            .andExpect(jsonPath("$.data.cartItems[0].productName").value("마우스"));
    }

    @Test
    @DisplayName("선택한 장바구니 상품 목록 조회 API가 정상적으로 응답한다.")
    void getSelectedItems_success() throws Exception {
        GetCartItemResponseDto item = new GetCartItemResponseDto(10L, 100L, "마우스", 10000, 2, 50);
        GetCartResponseDto mockResponse = new GetCartResponseDto(List.of(item), 20000);

        given(cartService.getSelectedItems(eq(1L), anyList())).willReturn(mockResponse);

        mockMvc.perform(get("/api/cartitems/selected")
                .param("ids", "10", "11") // ?ids=10,11
                .with(user(mockUser)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalPrice").value(20000));
    }

    @Test
    @DisplayName("장바구니 상품 수량 변경 API가 정상적으로 응답한다.")
    void updateQuantity_success() throws Exception {
        UpdateCartRequestDto request = new UpdateCartRequestDto(100L, 5);

        mockMvc.perform(patch("/api/cartitems")
                .with(user(mockUser))
                .with(csrf()) // PATCH 요청도 CSRF 방어가 필요함
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("수량이 성공적으로 변경되었습니다."));
    }

    @Test
    @DisplayName("장바구니 특정 상품 삭제 API가 정상적으로 응답한다.")
    void removeItem_success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/cartitems/{cartItemId}", 10L)
                .with(user(mockUser))
                .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("상품이 장바구니에서 삭제되었습니다."));
    }

    @Test
    @DisplayName("장바구니 전체 비우기 API가 정상적으로 응답한다.")
    void removeCart_success() throws Exception {
        mockMvc.perform(delete("/api/cartitems")
                .with(user(mockUser))
                .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("장바구니가 모두 비워졌습니다."));
    }
}
