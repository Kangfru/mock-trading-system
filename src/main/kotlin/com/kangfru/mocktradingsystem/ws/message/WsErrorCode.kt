package com.kangfru.mocktradingsystem.ws.message

/**
 * WebSocket 에러 코드 정의
 */
enum class WsErrorCode(val message: String) {
    // 프로토콜 에러
    INVALID_MESSAGE("잘못된 메시지 형식입니다"),
    MISSING_FIELD("필수 필드가 누락되었습니다"),

    // Rate Limiting
    RATE_LIMIT_EXCEEDED("요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요"),

    // 비즈니스 로직 에러
    ORDER_NOT_FOUND("주문을 찾을 수 없습니다"),
    ACCOUNT_NOT_FOUND("계좌를 찾을 수 없습니다"),
    INSUFFICIENT_BALANCE("잔고가 부족합니다"),
    INVALID_ORDER_STATUS("주문 상태가 올바르지 않습니다"),

    // 시스템 에러
    INTERNAL_ERROR("서버 내부 오류가 발생했습니다");

    fun toErrorResponse(requestId: String, detail: String? = null): WsErrorResponse =
        WsErrorResponse(
            requestId = requestId,
            errorCode = this.name,
            errorMessage = detail ?: this.message
        )
}
