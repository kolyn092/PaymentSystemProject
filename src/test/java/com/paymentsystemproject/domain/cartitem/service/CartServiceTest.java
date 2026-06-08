package com.paymentsystemproject.domain.cartitem.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.paymentsystemproject.domain.cartitem.dto.AddCartRequestDto;
import com.paymentsystemproject.domain.cartitem.dto.AddCartResponseDto;
import com.paymentsystemproject.domain.cartitem.dto.GetCartResponseDto;
import com.paymentsystemproject.domain.cartitem.dto.UpdateCartRequestDto;
import com.paymentsystemproject.domain.cartitem.entity.CartItem;
import com.paymentsystemproject.domain.cartitem.repository.CartItemRepository;
import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.member.repository.MemberRepository;
import com.paymentsystemproject.domain.product.entity.Product;
import com.paymentsystemproject.domain.product.entity.ProductCategory;
import com.paymentsystemproject.domain.product.entity.ProductStatus;
import com.paymentsystemproject.domain.product.repository.ProductRepository;
import com.paymentsystemproject.global.error.BusinessException;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

	@Mock
	private CartItemRepository cartItemRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private MemberRepository memberRepository;

	@InjectMocks
	private CartService cartService;

	private void setBaseTime(Object entity) {
		ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.now());
		ReflectionTestUtils.setField(entity, "updatedAt", LocalDateTime.now());
	}

	@Test
	@DisplayName("장바구니에 새로운 상품을 추가한다.")
	void addItem_NewItem_Success() {
		Long memberId = 1L;
		AddCartRequestDto requestDto = new AddCartRequestDto(100L, 2);

		Member member = mock(Member.class);
		Product product = Product.from("테스트 상품", 10000, 50, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONIC);
		ReflectionTestUtils.setField(product, "id", 100L);
		setBaseTime(product);

		CartItem savedCartItem = CartItem.from(member, product, 2);
		ReflectionTestUtils.setField(savedCartItem, "id", 10L);
		setBaseTime(savedCartItem);

		given(cartItemRepository.findByMember_idAndProduct_Id(memberId, 100L)).willReturn(Optional.empty());
		given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
		given(productRepository.findById(100L)).willReturn(Optional.of(product));
		given(cartItemRepository.save(any(CartItem.class))).willReturn(savedCartItem);

		AddCartResponseDto response = cartService.addItem(memberId, requestDto);

		assertThat(response.cartItemId()).isEqualTo(10L);
		assertThat(response.createdAt()).isNotNull();
		verify(cartItemRepository, times(1)).save(any(CartItem.class));
	}

	@Test
	@DisplayName("이미 장바구니에 있는 상품을 추가하면 수량만 증가한다.")
	void addItem_ExistingItem_Success() {
		Long memberId = 1L;
		AddCartRequestDto requestDto = new AddCartRequestDto(100L, 3);

		Member member = mock(Member.class);
		Product product = Product.from("테스트 상품", 10000, 50, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONIC);
		ReflectionTestUtils.setField(product, "id", 100L);
		setBaseTime(product);

		CartItem existingCartItem = CartItem.from(member, product, 2);
		ReflectionTestUtils.setField(existingCartItem, "id", 10L);
		setBaseTime(existingCartItem);

		given(cartItemRepository.findByMember_idAndProduct_Id(memberId, 100L)).willReturn(
			Optional.of(existingCartItem));

		AddCartResponseDto response = cartService.addItem(memberId, requestDto);

		assertThat(response.cartItemId()).isEqualTo(10L);
		assertThat(existingCartItem.getQuantity()).isEqualTo(5);
		verify(cartItemRepository, never()).save(any(CartItem.class));
	}

	@Test
	@DisplayName("장바구니 전체 조회 시 총 가격이 올바르게 계산된다.")
	void getCartItems_Success() {
		Long memberId = 1L;
		Member member = mock(Member.class);

		Product p1 = Product.from("마우스", 10000, 50, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONIC);
		ReflectionTestUtils.setField(p1, "id", 101L);
		setBaseTime(p1);

		Product p2 = Product.from("키보드", 50000, 50, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONIC);
		ReflectionTestUtils.setField(p2, "id", 102L);
		setBaseTime(p2);

		CartItem item1 = CartItem.from(member, p1, 2);
		ReflectionTestUtils.setField(item1, "id", 1L);
		setBaseTime(item1);

		CartItem item2 = CartItem.from(member, p2, 1);
		ReflectionTestUtils.setField(item2, "id", 2L);
		setBaseTime(item2);

		given(cartItemRepository.findByMemberId(memberId)).willReturn(List.of(item1, item2));

		GetCartResponseDto response = cartService.getCartItems(memberId);

		assertThat(response.cartItems()).hasSize(2);
		assertThat(response.totalPrice()).isEqualTo(70000);
		assertThat(response.cartItems().get(0).createdAt()).isNotNull();
	}

	@Test
	@DisplayName("장바구니 상품 수량을 변경할 수 있다.")
	void updateQuantity_Success() {
		Long memberId = 1L;
		UpdateCartRequestDto requestDto = new UpdateCartRequestDto(100L, 5);

		Member member = mock(Member.class);
		Product product = Product.from("테스트 상품", 10000, 50, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONIC);
		setBaseTime(product);

		CartItem item = CartItem.from(member, product, 1);
		setBaseTime(item);

		given(cartItemRepository.findByMember_idAndProduct_Id(memberId, 100L)).willReturn(Optional.of(item));

		cartService.updateQuantity(memberId, requestDto);

		assertThat(item.getQuantity()).isEqualTo(5);
	}

	@Test
	@DisplayName("장바구니 상품 삭제 시 삭제된 행이 0이면 예외가 발생한다.")
	void removeItem_Fail_NotFound() {
		Long memberId = 1L;
		Long cartItemId = 999L;

		given(cartItemRepository.deleteByIdAndMember_Id(cartItemId, memberId)).willReturn(0);

		assertThatThrownBy(() -> cartService.removeItem(memberId, cartItemId))
			.isInstanceOf(BusinessException.class);
	}
}
