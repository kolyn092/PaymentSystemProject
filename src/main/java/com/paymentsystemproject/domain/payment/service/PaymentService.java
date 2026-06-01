package com.paymentsystemproject.domain.payment.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.domain.payment.repository.PaymentRepository;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    // order 검증이 완료되었다는 가정 하에 작성
    // usedPoint = 사용자가 사용할 포인트 / availablePoint = 사용자가 보유하고 있는 포인트
    @Transactional
    public Payment createPayment(Order order, Integer usedPoint, Integer availablePoint) {
        // NOTE - 포인트 검증을 밖에서 하셨으면 굳이 사용하지 않으셔도 됩니다.
        validPoint(availablePoint, order.getTotalAmount(), usedPoint);

        // PortOne Id 생성
        String portonePaymentId = "payment-" + UUID.randomUUID();

        // pgAmount 계산
        Integer pgAmount = order.getTotalAmount() - usedPoint;

        Payment payment = Payment.createPendingPayment(order, portonePaymentId, order.getTotalAmount(), usedPoint,
            pgAmount);

        return paymentRepository.save(payment);
    }

    private void validPoint(Integer availablePoint, Integer totalAmount, Integer point) {
        // NOTE - dto 에서 처리를 한 경우 굳이 사용하지 않으셔도 됩니다.
        if (point < 0) { // 사용할 포인트가 0미만인 경우
            throw new BusinessException(ErrorCode.MINUS_POINT);
        }

        if (point > totalAmount) { // 주문 금액 이상으로 포인트를 사용한 경우
            throw new BusinessException(ErrorCode.POINT_EXCEEDS_ORDER_AMOUNT);
        }
        if (point > availablePoint) { // 보유 포인트 이상으로 포인트를 사용한 경우
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }
    }
}
