package com.paymentsystemproject.domain.order.entity;

import com.paymentsystemproject.domain.product.entity.Product;
import com.paymentsystemproject.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 항목 엔티티입니다.
 * 주문 생성 시점의 상품명과 가격을 스냅샷으로 저장하여,
 * 이후 상품 정보가 변경되더라도 주문 당시의 정보를 유지합니다.
 */

@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 주문 시점의 상품명 스냅샷
    @Column(name = "product_name", length = 200, nullable = false)
    private String productName;

    // 주문 시점의 상품 가격 스냅샷
    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer quantity;

    public OrderItem(Order order, Product product, String productName, Integer price, Integer quantity) {
        this.order = order;
        this.product = product;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }
}
