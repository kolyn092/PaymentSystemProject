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
import com.paymentsystemproject.domain.product.entity.ProductCategory;
import com.paymentsystemproject.domain.product.entity.ProductStatus;

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
	private ProductRepository productRepository;

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
	@DisplayName("이름 조건에 맞는 상품만 조회할 수 있다.")
	void findAll_withNameSpecification() {
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

	@Test
	@DisplayName("카테고리 조건으로 상품을 필터링할 수 있다.")
	void findAll_byCategory() {
		Product product1 = Product.from("키보드", 50000, 10, "설명", null, ProductCategory.ELECTRONIC);
		Product product2 = Product.from("사과", 5000, 50, "설명", null, ProductCategory.FOOD);
		productRepository.saveAll(List.of(product1, product2));

		Specification<Product> spec = (root, query, cb) -> cb.equal(root.get("category"), ProductCategory.ELECTRONIC);
		PageRequest pageRequest = PageRequest.of(0, 10);

		Page<Product> result = productRepository.findAll(spec, pageRequest);

		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).getName()).isEqualTo("키보드");
	}

	@Test
	@DisplayName("최소 가격과 최대 가격 조건으로 범위 내의 상품만 필터링할 수 있다.")
	void findAll_byPriceRange() {
		Product p1 = Product.from("싼 마우스", 10000, 10, "설명", null, null);
		Product p2 = Product.from("적당한 키보드", 50000, 10, "설명", null, null);
		Product p3 = Product.from("비싼 모니터", 200000, 10, "설명", null, null);
		productRepository.saveAll(List.of(p1, p2, p3));

		Specification<Product> spec = Specification.where((root, query, cb) -> cb.isTrue(cb.literal(true)));
		spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), 30000));
		spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), 100000));

		PageRequest pageRequest = PageRequest.of(0, 10);

		Page<Product> result = productRepository.findAll(spec, pageRequest);

		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).getName()).isEqualTo("적당한 키보드");
	}

	@Test
	@DisplayName("상태(Status) 조건으로 상품을 필터링할 수 있다.")
	void findAll_byStatus() {
		Product p1 = Product.from("판매중인 상품", 10000, 10, "설명", ProductStatus.ON_SALE, null);
		Product p2 = Product.from("품절된 상품", 10000, 0, "설명", ProductStatus.SOLD_OUT, null);
		productRepository.saveAll(List.of(p1, p2));

		Specification<Product> spec = (root, query, cb) -> cb.equal(root.get("status"), ProductStatus.ON_SALE);
		PageRequest pageRequest = PageRequest.of(0, 10);

		Page<Product> result = productRepository.findAll(spec, pageRequest);

		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).getName()).isEqualTo("판매중인 상품");
	}

	@Test
	@DisplayName("카테고리와 가격 범위를 조합하여 복합 조건으로 필터링할 수 있다.")
	void findAll_byComplexConditions() {
		Product p1 = Product.from("비싼 전자기기", 200000, 10, "설명", null, ProductCategory.ELECTRONIC);
		Product p2 = Product.from("싼 전자기기", 15000, 10, "설명", null, ProductCategory.ELECTRONIC);
		Product p3 = Product.from("비싼 음식", 150000, 10, "설명", null, ProductCategory.FOOD);
		productRepository.saveAll(List.of(p1, p2, p3));

		Specification<Product> spec = Specification.where((root, query, cb) -> cb.isTrue(cb.literal(true)));
		spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), ProductCategory.ELECTRONIC));
		spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), 50000));

		PageRequest pageRequest = PageRequest.of(0, 10);

		Page<Product> result = productRepository.findAll(spec, pageRequest);

		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).getName()).isEqualTo("비싼 전자기기");
	}
}
