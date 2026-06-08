package com.paymentsystemproject.global.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "입력값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 내부 오류가 발생했습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 토큰입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_003", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_004", "이메일 또는 비밀번호가 올바르지 않습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_005", "회원을 찾을 수 없습니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_051", "상품을 찾을 수 없습니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "PRODUCT_052", "재고가 부족합니다."),

    // CartItem
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_101", "장바구니 항목을 찾을 수 없습니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "CART_102", "수량은 1 이상이어야 합니다."),
    EXCEED_STOCK(HttpStatus.BAD_REQUEST, "CART_103", "상품의 재고 수량을 초과하여 담을 수 없습니다."),

    // Payment
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "PAYMENT_001", "보유 포인트가 부족합니다."),
    MINUS_POINT(HttpStatus.BAD_REQUEST, "PAYMENT_002", "포인트는 음수일 수 없습니다."),
    PAYMENT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "PAYMENT_003", "변경할 수 없는 결제 상태입니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_004", "결제를 찾을 수 없습니다."),
    ALREADY_PROCESSED_PAYMENT(HttpStatus.BAD_REQUEST, "PAYMENT_005", "이미 결제를 완료하였습니다."),
    PAYMENT_NOT_PAID(HttpStatus.BAD_REQUEST, "PAYMENT_006", "결제가 완료되지 않았습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT_007", "결제 금액이 불일치합니다."),
    PAYMENT_ALREADY_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_008", "이미 실패한 결제입니다."),
    PAYMENT_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "PAYMENT_009", "이미 취소된 결제입니다."),
    POINT_EXCEEDS_ORDER_AMOUNT(HttpStatus.BAD_REQUEST, "PAYMENT_010", "주문 금액보다 많이 사용할 수 없습니다."),
    PORTONE_SECRET_NOT_FOUND(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "PAYMENT_011",
        "PortOne API Secret이 설정되어 있지 않습니다."
    ),
    PG_REFUND_FAILED(HttpStatus.BAD_GATEWAY, "PAYMENT_012", "PG 환불 요청에 실패했습니다."),

    // Webhook
    WEBHOOK_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "WEBHOOK_001", "주문 금액보다 많이 사용할 수 없습니다."),
    WEBHOOK_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "WEBHOOK_002", "웹훅 서명 인증에 실패하였습니다."),

    // Order
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "ORDER_001", "접근 권한이 없습니다."),
    CART_EMPTY(HttpStatus.BAD_REQUEST, "ORDER_002", "장바구니가 비어 있습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_003", "주문을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "ORDER_004", "유효하지 않은 주문 상태입니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ORDER_005", "본인 주문만 환불할 수 있습니다."),
    ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_006", "주문 상품을 찾을 수 없습니다."),
    ORDER_NOT_PAID(HttpStatus.BAD_REQUEST, "ORDER_007", "결제 완료된 주문만 환불할 수 있습니다."),

    // Refund
    EXCEED_REFUNDABLE_QUANTITY(HttpStatus.BAD_REQUEST, "REFUND_001", "잔여 환불 가능 수량을 초과했습니다."),
    ALREADY_REFUNDED(HttpStatus.BAD_REQUEST, "REFUND_002", "이미 전체 환불된 주문입니다."),
    DUPLICATE_REFUND_ITEM(HttpStatus.BAD_REQUEST, "REFUND_003", "동일한 주문 상품은 한 번만 환불 요청할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
