package com.paymentsystemproject.domain.order.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.order.entity.OrderItem;
import com.paymentsystemproject.domain.product.entity.Product;
import com.paymentsystemproject.domain.product.entity.ProductCategory;
import com.paymentsystemproject.domain.product.entity.ProductStatus;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@Sql("/schema-test.sql")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private OrderRepository orderRepository;

    private Member member;
    private Member other;
    private Order order;

    @BeforeEach
    void setUp() {
        Product product = Product.from("테스트상품", 10000, 100, "설명", ProductStatus.ON_SALE, ProductCategory.FOOD);
        em.persist(product);

        member = new Member("test@test.com", "password", "테스트유저", "010-1234-5678");
        em.persist(member);

        other = new Member("other@test.com", "password", "다른유저", "010-9999-9999");
        em.persist(other);

        // findByIdAndMemberIdWithOrderItems 는 JOIN FETCH (inner join) 사용
        // → OrderItem이 없으면 결과가 반환되지 않으므로 반드시 추가
        OrderItem orderItem = new OrderItem(product, "테스트상품", 10000, 1);
        order = new Order(member, UUID.randomUUID().toString(), 10000, List.of(orderItem));
        em.persist(order);

        em.flush();
        em.clear();
    }

    // ===== findByMember =====

    @Test
    @DisplayName("findByMember - 회원의 주문 목록 반환")
    void findByMember_성공() {
        // given
        Member foundMember = em.find(Member.class, member.getId());
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Order> result = orderRepository.findByMember(foundMember, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTotalAmount()).isEqualTo(10000);
    }

    @Test
    @DisplayName("findByMember - 주문 없는 회원은 빈 목록 반환")
    void findByMember_빈목록() {
        // given
        Member foundOther = em.find(Member.class, other.getId());
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Order> result = orderRepository.findByMember(foundOther, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("findByMember - 페이지네이션 동작 확인")
    void findByMember_페이지네이션() {
        // given
        Member foundMember = em.find(Member.class, member.getId());
        for (int i = 0; i < 2; i++) {
            em.persist(new Order(foundMember, UUID.randomUUID().toString(), 5000, List.of()));
        }
        em.flush();
        em.clear();

        foundMember = em.find(Member.class, member.getId());
        PageRequest pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Order> result = orderRepository.findByMember(foundMember, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    // ===== findByIdAndMemberIdWithOrderItems =====

    @Test
    @DisplayName("findByIdAndMemberIdWithOrderItems - 성공 (OrderItem JOIN FETCH 포함)")
    void findByIdAndMemberIdWithOrderItems_성공() {
        // when
        Optional<Order> result = orderRepository.findByIdAndMemberIdWithOrderItems(
            order.getId(), member.getId()
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTotalAmount()).isEqualTo(10000);
        assertThat(result.get().getOrderItems()).hasSize(1);
        assertThat(result.get().getOrderItems().get(0).getProductName()).isEqualTo("테스트상품");
    }

    @Test
    @DisplayName("findByIdAndMemberIdWithOrderItems - 없는 orderId면 빈 Optional 반환")
    void findByIdAndMemberIdWithOrderItems_없는orderId() {
        // when
        Optional<Order> result = orderRepository.findByIdAndMemberIdWithOrderItems(
            9999L, member.getId()
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndMemberIdWithOrderItems - 다른 memberId면 빈 Optional 반환 (소유권 검증)")
    void findByIdAndMemberIdWithOrderItems_다른memberId() {
        // when
        Optional<Order> result = orderRepository.findByIdAndMemberIdWithOrderItems(
            order.getId(), other.getId()
        );

        // then
        assertThat(result).isEmpty();
    }
}
