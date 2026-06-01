package com.paymentsystemproject.domain.order.entity;

import com.paymentsystemproject.domain.member.entity.Member;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "orders",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_orders_order_number",
        columnNames = "order_number"
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "order_number", length = 50, nullable = false)
    private String orderNumber;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(length = 20, nullable = false)
    private String status;
}
