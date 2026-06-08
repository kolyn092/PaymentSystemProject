package com.paymentsystemproject.domain.order.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateRefundRequestDto(
    @NotEmpty(message = "환불 상품 목록은 필수입니다.")
    @Valid
    List<RefundItemRequestDto> items,

    @NotBlank(message = "환불 사유는 필수입니다.")
    @Size(max = 500, message = "환불 사유는 500자 이하여야 합니다.")
    String reason
) {

    public record RefundItemRequestDto(
        @NotNull(message = "주문 상품 ID는 필수입니다.")
        Long orderItemId,

        @NotNull(message = "환불 수량은 필수입니다.")
        @Positive(message = "환불 수량은 1개 이상이어야 합니다.")
        Integer quantity
    ) {
    }
}
