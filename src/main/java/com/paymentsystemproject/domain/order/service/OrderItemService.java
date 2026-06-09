package com.paymentsystemproject.domain.order.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentsystemproject.domain.order.entity.OrderItem;
import com.paymentsystemproject.domain.order.repository.OrderItemRepository;
import com.paymentsystemproject.domain.order.repository.RefundItemRepository;
import com.paymentsystemproject.global.error.BusinessException;
import com.paymentsystemproject.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final RefundItemRepository refundItemRepository;

    public OrderItem findOrderItemForRefund(Long orderItemId, Long orderId) {
        return orderItemRepository.findByIdAndOrder_Id(orderItemId, orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));
    }

    public void validateRefundableQuantity(OrderItem orderItem, Integer requestQuantity) {
        Integer refundedQuantity = refundItemRepository.sumRefundedQuantityByOrderItemId(orderItem.getId());
        Integer refundableQuantity = orderItem.getQuantity() - refundedQuantity;

        if (requestQuantity > refundableQuantity) {
            throw new BusinessException(ErrorCode.EXCEED_REFUNDABLE_QUANTITY);
        }
    }

    @Transactional
    public void restoreStock(List<OrderItemRefundStockRestoreCommand> commands) {
        for (OrderItemRefundStockRestoreCommand command : commands) {
            command.orderItem().getProduct().increaseStock(command.quantity());
        }
    }

    public record OrderItemRefundStockRestoreCommand(
        OrderItem orderItem,
        Integer quantity
    ) {
    }

}
