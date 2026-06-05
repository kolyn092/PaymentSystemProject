package com.paymentsystemproject.domain.order.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.member.entity.Member;
import com.paymentsystemproject.domain.order.dto.CancelOrderResponseDto;
import com.paymentsystemproject.domain.order.dto.GetOrderListResponseDto;
import com.paymentsystemproject.domain.order.entity.Order;
import com.paymentsystemproject.domain.order.entity.OrderItem;
import com.paymentsystemproject.domain.order.entity.OrderStatus;
import com.paymentsystemproject.domain.order.repository.OrderRepository;
import com.paymentsystemproject.domain.payment.entity.Payment;
import com.paymentsystemproject.domain.payment.entity.PaymentStatus;
import com.paymentsystemproject.domain.payment.repository.PaymentRepository;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 주문 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 주문서 미리보기, 주문 생성(결제 준비), 주문 내역 조회 및 취소 등의 기능을 제공합니다.
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    /**
     * 주문을 생성하고 저장합니다.
     *
     * @param member 회원 엔티티
     * @param orderItems 주문 항목 목록
     * @param totalAmount 주문 총액
     * @return 생성된 주문 엔티티
     */

    @Transactional
    public Order createOrder(Member member, List<OrderItem> orderItems, Integer totalAmount) {
        Order order = new Order(member, UUID.randomUUID().toString(), totalAmount, orderItems);
        return orderRepository.save(order);
    }

    /**
     * 로그인한 회원의 주문 목록을 최신순으로 페이징하여 반환합니다.
     *
     * @param member 회원 엔티티
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 항목 수
     * @return 주문 목록 (페이징)
     */

    public Page<GetOrderListResponseDto> getOrderList(Member member, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findByMember(member, pageable)
            .map(GetOrderListResponseDto::from);
    }

    /**
     * 주문 ID와 회원 ID로 주문과 주문 항목을 함께 조회합니다.
     *
     * @param orderId 주문 ID
     * @param memberId 회원 ID
     * @return 주문 항목이 포함된 주문 엔티티
     */

    public Order getOrderDetail(Long memberId, Long orderId) {
        return orderRepository.findByIdAndMemberIdWithOrderItems(memberId, orderId);
    }

    /**
     * 결제 대기 상태인 주문을 취소합니다.
     * orderId와 memberId를 함께 조회하여 본인 부문만 취소할 수 있도록 보장합니다.
     * 취소 시 선차감했던 재고를 복구하고, 결제 상태를 FAILED로 변경합니다.
     *
     * @param memberId 회원 ID
     * @param orderId 취소할 주문 ID
     * @return 취소된 주문 정보
     */

    @Transactional
    public CancelOrderResponseDto cancelOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findByIdAndMemberIdWithOrderItems(orderId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Payment payment = paymentRepository.findByOrder(order)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        for (OrderItem orderItem : order.getOrderItems()) {
            orderItem.getProduct().restoreStock(orderItem.getQuantity());
        }
        payment.changeStatus(PaymentStatus.FAILED);

        order.cancel();

        return CancelOrderResponseDto.from(order);
    }
}
