package com.kangfru.mocktradingsystem.ws.message

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.kangfru.mocktradingsystem.domain.OrderType
import com.kangfru.mocktradingsystem.domain.PriceType
import java.math.BigDecimal

/**
 * WebSocket 요청 메시지의 base sealed class
 *
 * Jackson이 "type" 필드를 보고 자동으로 알맞은 하위 클래스로 역직렬화합니다.
 * 예: {"type": "CREATE_ORDER", "requestId": "abc", ...} → WsCreateOrderRequest
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = WsCreateOrderRequest::class, name = "CREATE_ORDER"),
    JsonSubTypes.Type(value = WsQueryOrderRequest::class, name = "QUERY_ORDER"),
    JsonSubTypes.Type(value = WsCancelOrderRequest::class, name = "CANCEL_ORDER"),
    JsonSubTypes.Type(value = WsGetPortfolioRequest::class, name = "GET_PORTFOLIO")
)
sealed class WsRequest {
    abstract val requestId: String
}

/**
 * 주문 생성 요청
 */
data class WsCreateOrderRequest(
    override val requestId: String,
    val stockCode: String,
    val orderType: OrderType,
    val quantity: Int,
    val price: BigDecimal,
    val priceType: PriceType
) : WsRequest()

/**
 * 주문 조회 요청
 */
data class WsQueryOrderRequest(
    override val requestId: String,
    val orderNumber: Long
) : WsRequest()

/**
 * 주문 취소 요청
 */
data class WsCancelOrderRequest(
    override val requestId: String,
    val orderNumber: Long,
    val originalOrderNumber: Long,
    val stockCode: String,
    val orderType: OrderType,
    val quantity: Int,
    val price: BigDecimal
) : WsRequest()

/**
 * 포트폴리오(보유 자산) 조회 요청
 */
data class WsGetPortfolioRequest(
    override val requestId: String,
    val accountNumber: String
) : WsRequest()
