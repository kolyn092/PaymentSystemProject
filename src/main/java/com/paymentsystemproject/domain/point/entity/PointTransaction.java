package com.paymentsystemproject.domain.point.entity;

import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.payment.entity.Payment;
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
@Table(name = "point_transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(length = 20, nullable = false)
    private String type;

    @Column(nullable = false)
    private Integer amount;

    private PointTransaction(Member member, Payment payment, String type, Integer amount) {
        this.member = member;
        this.payment = payment;
        this.type = type;
        this.amount = amount;
    }

    public static PointTransaction createRefundUsePoint(
        Member member,
        Payment payment,
        Integer amount
    ) {
        return new PointTransaction(
            member,
            payment,
            "REFUND_USE",
            amount
        );
    }

    public static PointTransaction createCancelEarnPoint(
        Member member,
        Payment payment,
        Integer amount
    ) {
        return new PointTransaction(
            member,
            payment,
            "CANCEL_EARN",
            -amount
        );
    }
}
