package com.paymentsystemproject.domain.order.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.paymentsystemproject.domain.payment.entity.Payment;

@Component
public class RefundCalculator {

    public Integer calculateItemRefundAmount(Integer itemPrice, Integer requestQuantity) {
        return itemPrice * requestQuantity;
    }

    public Integer calculatePointRefundAmount(Payment payment, Integer refundAmount) {
        if (payment.getTotalAmount() == 0) {
            return 0;
        }

        return refundAmount * payment.getUsePoint() / payment.getTotalAmount();
    }

    public Integer calculatePgRefundAmount(Integer refundAmount, Integer pointRefundAmount) {
        return refundAmount - pointRefundAmount;
    }

    public Integer calculateCancelEarnPoint(Payment payment, Integer refundAmount) {
        if (payment.getTotalAmount() == 0) {
            return 0;
        }

        return refundAmount * payment.getEarnedPoint() / payment.getTotalAmount();
    }

    public Integer calculateTotalPointRefundAmount(List<RefundItemCalculationResult> calculations) {
        return calculations.stream()
            .mapToInt(RefundItemCalculationResult::pointRefundAmount)
            .sum();
    }

    public Integer calculateTotalPgRefundAmount(List<RefundItemCalculationResult> calculations) {
        return calculations.stream()
            .mapToInt(RefundItemCalculationResult::pgRefundAmount)
            .sum();
    }

    public Integer calculateTotalRefundAmount(List<RefundItemCalculationResult> calculations) {
        return calculations.stream()
            .mapToInt(RefundItemCalculationResult::totalRefundAmount)
            .sum();
    }

    public record RefundItemCalculationResult(Integer pointRefundAmount, Integer pgRefundAmount) {
        public Integer totalRefundAmount() {
            return pointRefundAmount + pgRefundAmount;
        }
    }
}
