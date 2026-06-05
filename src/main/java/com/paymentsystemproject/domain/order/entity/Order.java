package com.paymentsystemproject.domain.order.entity;

import java.util.ArrayList;
import java.util.List;

import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.global.entity.BaseTimeEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 엔티티입니다.
 * 회원이 장바구니에서 선택한 상품들을 기반으로 생성되며,
 * 주문 번호는 UUID로 생성되어 중복을 방지합니다.
 * 주문 상태는 PENDING_PAYMENT(결제 대기)로 초기화됩니다.
 */

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
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    public Order(Member member, String orderNumber, Integer totalAmount, List<OrderItem> orderItems) {
        this.member = member;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        // 주문 생성 시 결제 대기 상태로 초기화
        this.status = OrderStatus.PENDING_PAYMENT;
        for (OrderItem orderItem : orderItems) {
            addOrderitem(orderItem);
        }
    }

    public void addOrderitem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    /**
     * 주문을 취소 상태로 변경합니다.
     * 결제 대기 (PENDING_PAYMENT) 상태인 경우에만 호출되어야 합니다.
     */

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * 주문을 완료 상태로 변경합니다.
     * 결제 확정 (PAID) 시점에 호출되어야 합니다.
     */

    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }
}
