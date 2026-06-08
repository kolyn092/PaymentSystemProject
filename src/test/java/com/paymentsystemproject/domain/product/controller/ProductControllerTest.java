package com.paymentsystemproject.domain.product.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.paymentsystemproject.domain.product.dto.GetOneProductResponseDto;
import com.paymentsystemproject.domain.product.dto.GetProductListResponseDto;
import com.paymentsystemproject.domain.product.dto.GetProductPageableResponseDto;
import com.paymentsystemproject.domain.product.service.ProductService;
import com.paymentsystemproject.global.status.ProductCategory;
import com.paymentsystemproject.global.status.ProductStatus;

@WebMvcTest(ProductController.class)
@WithMockUser
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    @DisplayName("상품 목록 조회 API가 정상적으로 응답한다.")
    void list_success() throws Exception {
        GetProductListResponseDto item = new GetProductListResponseDto(
            1L, "기계식 키보드", 150000, 10, ProductStatus.ON_SALE, ProductCategory.ELECTRONIC
        );
        GetProductPageableResponseDto mockResponse = new GetProductPageableResponseDto(
            List.of(item), 0, 10, 1, 1
        );

        given(productService.findAll(any(), any(), any(), any(), any())).willReturn(mockResponse);

        mockMvc.perform(get("/api/products")
                .param("category", "ELECTRONIC")
                .param("page", "0")
                .param("size", "10"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].id").value(1L))
            .andExpect(jsonPath("$.data.content[0].name").value("기계식 키보드"))
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("상품 상세 조회 API가 정상적으로 응답한다.")
    void detail_success() throws Exception {
        GetOneProductResponseDto mockResponse = new GetOneProductResponseDto(
            1L, "기계식 키보드", 150000, 10, "타건감이 좋습니다.", ProductStatus.ON_SALE, ProductCategory.ELECTRONIC
        );

        given(productService.findOneProduct(1L)).willReturn(mockResponse);

        mockMvc.perform(get("/api/products/{id}", 1L)) // PathVariable 흉내 내기 (/api/products/1)
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1L))
            .andExpect(jsonPath("$.data.name").value("기계식 키보드"))
            .andExpect(jsonPath("$.data.price").value(150000));
    }
}
