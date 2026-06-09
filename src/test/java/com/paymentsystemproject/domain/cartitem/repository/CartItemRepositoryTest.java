package com.paymentsystemproject.domain.cartitem.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import com.paymentsystemproject.domain.cartitem.entity.CartItem;
import com.paymentsystemproject.domain.member.entity.Member;
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
class CartItemRepositoryTest {

	@Autowired
	private CartItemRepository cartItemRepository;

	@Autowired
	private TestEntityManager em;

	private Member testMember;
	private Product testProduct1;
	private Product testProduct2;

	@BeforeEach
	void setUp() {
		testMember = new Member("test@test.com", "password123", "테스터", "010-1234-5678");
		em.persist(testMember);

		testProduct1 = Product.from("상품1", 10000, 50, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONIC);
		testProduct2 = Product.from("상품2", 20000, 50, "설명", ProductStatus.ON_SALE, ProductCategory.FOOD);
		em.persist(testProduct1);
		em.persist(testProduct2);

		em.flush();
		em.clear();
	}

	@Test
	@DisplayName("회원 ID로 장바구니 목록을 조회하며, FETCH JOIN으로 상품 정보까지 한 번에 가져온다.")
	void findByMemberId() {
		CartItem item1 = CartItem.from(testMember, testProduct1, 2);
		CartItem item2 = CartItem.from(testMember, testProduct2, 5);
		cartItemRepository.saveAll(List.of(item1, item2));

		em.flush();
		em.clear();

		List<CartItem> result = cartItemRepository.findByMemberId(testMember.getId());

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getProduct().getName()).isEqualTo("상품1");
	}

	@Test
	@DisplayName("회원 ID와 상품 ID로 장바구니에 담긴 특정 상품을 찾을 수 있다.")
	void findByMember_idAndProduct_Id() {
		CartItem item = CartItem.from(testMember, testProduct1, 2);
		cartItemRepository.save(item);

		Optional<CartItem> result = cartItemRepository.findByMember_idAndProduct_Id(testMember.getId(),
			testProduct1.getId());

		assertThat(result).isPresent();
		assertThat(result.get().getQuantity()).isEqualTo(2);
	}

	@Test
	@DisplayName("회원 ID와 장바구니 아이템 ID 목록(IN 절)으로 선택된 장바구니 상품들을 조회한다.")
	void findByMemberIdAndIdIn() {
		CartItem item1 = cartItemRepository.save(CartItem.from(testMember, testProduct1, 1));
		CartItem item2 = cartItemRepository.save(CartItem.from(testMember, testProduct2, 2));

		List<Long> selectedIds = List.of(item1.getId(), item2.getId());
		List<CartItem> result = cartItemRepository.findByMemberIdAndIdIn(testMember.getId(), selectedIds);

		assertThat(result).hasSize(2);
		assertThat(result).extracting("id").containsExactlyInAnyOrder(item1.getId(), item2.getId());
	}

	@Test
	@DisplayName("장바구니 아이템 ID와 회원 ID로 특정 상품을 삭제하고, 삭제된 행의 갯수를 반환한다.")
	void deleteByIdAndMember_Id() {
		CartItem item = cartItemRepository.save(CartItem.from(testMember, testProduct1, 1));

		int deletedCount = cartItemRepository.deleteByIdAndMember_Id(item.getId(), testMember.getId());

		assertThat(deletedCount).isEqualTo(1);

		em.flush();
		em.clear();

		Optional<CartItem> findItem = cartItemRepository.findById(item.getId());
		assertThat(findItem).isEmpty();
	}

	@Test
	@DisplayName("회원 ID로 해당 회원의 장바구니를 모두 비우고 삭제된 행의 갯수를 반환한다.")
	void deleteAllByMemberId() {
		cartItemRepository.save(CartItem.from(testMember, testProduct1, 1));
		cartItemRepository.save(CartItem.from(testMember, testProduct2, 2));

		int deletedCount = cartItemRepository.deleteAllByMemberId(testMember.getId());

		assertThat(deletedCount).isEqualTo(2);

		List<CartItem> remainingItems = cartItemRepository.findByMemberId(testMember.getId());
		assertThat(remainingItems).isEmpty();
	}
}
