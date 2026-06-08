package com.paymentsystemproject.domain.order.entity;

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

@Entity
@Table(name = "refund_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refund_id", nullable = false)
    private Refund refund;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "point_refund_amount", nullable = false)
    private Integer pointRefundAmount;

    @Column(name = "pg_refund_amount", nullable = false)
    private Integer pgRefundAmount;

    private RefundItem(
        Refund refund,
        OrderItem orderItem,
        Integer quantity,
        Integer pointRefundAmount,
        Integer pgRefundAmount
    ) {
        this.refund = refund;
        this.orderItem = orderItem;
        this.quantity = quantity;
        this.pointRefundAmount = pointRefundAmount;
        this.pgRefundAmount = pgRefundAmount;
    }

    public static RefundItem createRefundItem(
        Refund refund,
        OrderItem orderItem,
        Integer quantity,
        Integer pointRefundAmount,
        Integer pgRefundAmount
    ) {
        return new RefundItem(
            refund,
            orderItem,
            quantity,
            pointRefundAmount,
            pgRefundAmount
        );
    }
}
