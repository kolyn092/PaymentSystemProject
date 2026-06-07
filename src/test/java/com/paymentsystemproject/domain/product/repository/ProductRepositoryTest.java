package com.paymentsystemproject.domain.product.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;

import com.paymentsystemproject.domain.product.entity.Product;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository; // 실제 빈(Bean)을 주입받음

    @Test
    @DisplayName("상품을 데이터베이스에 정상적으로 저장하고 조회할 수 있다.")
    void saveAndFindById() {
        Product product = Product.from("기계식 키보드", 150000, 10, "타건감이 좋습니다.", null, null);

        Product savedProduct = productRepository.save(product);
        Product foundProduct = productRepository.findById(savedProduct.getId()).orElse(null);

        assertThat(foundProduct).isNotNull();
        assertThat(foundProduct.getId()).isEqualTo(savedProduct.getId());
        assertThat(foundProduct.getName()).isEqualTo("기계식 키보드");
        assertThat(foundProduct.getPrice()).isEqualTo(150000);
    }

    @Test
    @DisplayName("조건에 맞는 상품만 조회할 수 있다.")
    void findAll_withSpecification() {
        Product product1 = Product.from("게이밍 마우스", 50000, 20, "설명1", null, null);
        Product product2 = Product.from("사무용 마우스", 20000, 50, "설명2", null, null);
        Product product3 = Product.from("모니터", 300000, 5, "설명3", null, null);

        productRepository.saveAll(List.of(product1, product2, product3));

        Specification<Product> spec = (root, query, cb) -> cb.like(root.get("name"), "%마우스%");
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<Product> result = productRepository.findAll(spec, pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting("name")
            .containsExactlyInAnyOrder("게이밍 마우스", "사무용 마우스");
    }
}
