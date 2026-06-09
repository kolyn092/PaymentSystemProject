package com.paymentsystemproject.domain.cartitem.entity;

import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.product.entity.Product;
import com.paymentsystemproject.global.entity.BaseTimeEntity;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_cart_item_cart_product",
        columnNames = {"member_id", "product_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    private CartItem(Member member, Product product, int quantity) {
        this.member = member;
        this.product = product;
        this.quantity = quantity;
    }

    public Long getMemberId() {
        return member.getId();
    }

    public Long getProductId() {
        return product.getId();
    }

    public static CartItem from(Member member, Product product, int quantity) {
        return new CartItem(member, product, quantity);
    }

    public void addQuantity(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
        if (this.quantity + quantity > this.product.getStock()) {
            throw new BusinessException(ErrorCode.EXCEED_STOCK);
        }
        this.quantity += quantity;
    }

    public void changeQuantity(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
        if (quantity > this.product.getStock()) {
            throw new BusinessException(ErrorCode.EXCEED_STOCK);
        }
        this.quantity = quantity;
    }

}
