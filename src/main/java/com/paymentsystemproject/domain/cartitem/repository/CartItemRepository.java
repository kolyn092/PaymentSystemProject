package com.paymentsystemproject.domain.cartitem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.paymentsystemproject.domain.cartitem.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 전체 조회에 사용
    @Query("""
        SELECT ci
        FROM CartItem ci
        JOIN FETCH ci.product
        WHERE ci.member.id = :memberId
        """)
    List<CartItem> findByMemberId(Long memberId);

    // 상품 추가할 때 사용
    Optional<CartItem> findByMember_idAndProduct_Id(Long memberId, Long productId);

    // 상품 단건 조회할 때 사용
    @Query("""
        SELECT ci
        FROM CartItem ci
        JOIN FETCH ci.product
        WHERE ci.member.id = :memberId AND ci.id IN :cartItemIds
        """)
    List<CartItem> findByMemberIdAndIdIn(@Param("memberId") Long memberId,
        @Param("cartItemIds") List<Long> cartItemIds);

    @Modifying
    @Query("""
        DELETE FROM CartItem ci
        WHERE ci.id = :id AND ci.member.id = :memberId
        """)
    int deleteByIdAndMember_Id(@Param("id") Long id, @Param("memberId") Long memberId);

    @Modifying
    @Query("""
        DELETE FROM CartItem ci
        WHERE ci.member.id = :memberId
        """)
    int deleteAllByMemberId(@Param("memberId") Long memberId);
}

// public interface CartItemRepository extends JpaRepository<CartItem, Long> {
//     @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.product WHERE ci.member.id = :memberId")
//     List<CartItem> findByMemberId(@Param("memberId") Long memberId);
//
//     Optional<CartItem> findByMember_IdAndProduct_Id(Long memberId, Long productId);
//
//     @Modifying
//     @Query("DELETE FROM CartItem ci WHERE ci.id = :id AND ci.member.id = :memberId")
//     int deleteByIdAndMember_Id(@Param("id") Long id, @Param("memberId") Long memberId);
//
// }

