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

    // Payment
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "PAYMENT_001", "보유 포인트가 부족합니다."),
    MINUS_POINT(HttpStatus.BAD_REQUEST, "PAYMENT_002", "포인트는 음수일 수 없습니다."),
    PAYMENT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "PAYMENT_003", "변경할 수 없는 결제 상태입니다."),
    POINT_EXCEEDS_ORDER_AMOUNT(HttpStatus.BAD_REQUEST, "PAYMENT_00", "주문 금액보다 많이 사용할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
