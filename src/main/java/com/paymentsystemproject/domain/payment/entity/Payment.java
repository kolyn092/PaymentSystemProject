package com.paymentsystemproject.domain.payment.entity;

import java.time.LocalDateTime;

import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "payment",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_payment_portone_payment_id",
        columnNames = "portone_payment_id"
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "portone_payment_id", length = 100, nullable = false)
    private String portonePaymentId;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "use_point", nullable = false)
    private Integer usePoint;

    @Column(name = "pg_amount", nullable = false)
    private Integer pgAmount; // 포인트를 제외한 결제해야 하는 금액

    @Column(name = "earned_point", nullable = false)
    private Integer earnedPoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    private Payment(Order order, String portonePaymentId, Integer totalAmount, Integer usePoint, Integer pgAmount,
        Integer earnedPoint,
        PaymentStatus status) {
        this.order = order;
        this.portonePaymentId = portonePaymentId;
        this.totalAmount = totalAmount;
        this.usePoint = usePoint;
        this.pgAmount = pgAmount;
        this.earnedPoint = earnedPoint;
        this.status = status;
    }

    public static Payment createPendingPayment(
        Order order,
        String portonePaymentId,
        Integer totalAmount,
        Integer usePoint,
        Integer pgAmount
    ) {
        return new Payment(
            order,
            portonePaymentId,
            totalAmount,
            usePoint,
            pgAmount,
            0,
            PaymentStatus.PENDING
        );
    }

    public void markAsCompleted() {
        changeStatus(PaymentStatus.COMPLETED);
        this.paidAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        changeStatus(PaymentStatus.FAILED);
    }

    public void markAsCanceled() {
        changeStatus(PaymentStatus.CANCELED);
    }

    public void changeStatus(PaymentStatus nextStatus) {
        this.status.validateTransition(nextStatus);
        this.status = nextStatus;
    }

}
