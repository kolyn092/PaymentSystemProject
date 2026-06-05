package com.paymentsystemproject.domain.product.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.paymentsystemproject.domain.product.dto.GetOneProductResponseDto;
import com.paymentsystemproject.domain.product.dto.GetProductPageableResponseDto;
import com.paymentsystemproject.domain.product.entity.Product;
import com.paymentsystemproject.domain.product.repository.ProductRepository;
import com.paymentsystemproject.global.error.BusinessException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("조건에 맞는 상품 목록을 페이징하여 조회할 수 있다.")
    void findAll_success() {
        Product product1 = Product.from("키보드", 150000, 10, "기계식 키보드", null, null);
        ReflectionTestUtils.setField(product1, "id", 1L);
        Product product2 = Product.from("마우스", 80000, 20, "무선 마우스", null, null);
        ReflectionTestUtils.setField(product2, "id", 2L);

        Pageable pageable = PageRequest.of(0, 10);

        List<Product> productList = List.of(product1, product2);
        Page<Product> mockPage = new PageImpl<>(productList, pageable, productList.size());

        given(productRepository.findAll(any(Specification.class), eq(pageable))).willReturn(mockPage);

        GetProductPageableResponseDto response = productService.findAll(
            "ELECTRONICS", 50000, 200000, "SELLING", pageable
        );

        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.pageNumber()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(10);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).name()).isEqualTo("키보드");
        assertThat(response.content().get(1).price()).isEqualTo(80000);
    }

    @Test
    @DisplayName("상품 ID로 단건 조회에 성공한다.")
    void findOneProduct_success() {
        Product product = Product.from("테스트 상품", 10000, 5, "설명", null, null);
        ReflectionTestUtils.setField(product, "id", 1L);

        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        GetOneProductResponseDto response = productService.findOneProduct(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("테스트 상품");
        assertThat(response.price()).isEqualTo(10000);
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 조회 시 예외가 발생한다.")
    void findOneProduct_fail_notFound() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findOneProduct(999L))
            .isInstanceOf(BusinessException.class);
    }
}
