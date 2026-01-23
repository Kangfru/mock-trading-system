package com.kangfru.mocktradingsystem.ws.message

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.kangfru.mocktradingsystem.domain.Order
import com.kangfru.mocktradingsystem.domain.StockHolding
import java.math.BigDecimal

/**
 * WebSocket 응답 메시지의 base sealed class
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = WsOrderCreatedResponse::class, name = "ORDER_CREATED"),
    JsonSubTypes.Type(value = WsOrderQueriedResponse::class, name = "ORDER_QUERIED"),
    JsonSubTypes.Type(value = WsOrderCancelledResponse::class, name = "ORDER_CANCELLED"),
    JsonSubTypes.Type(value = WsPortfolioResponse::class, name = "PORTFOLIO"),
    JsonSubTypes.Type(value = WsErrorResponse::class, name = "ERROR")
)
sealed class WsResponse {
    abstract val requestId: String
    abstract val success: Boolean
}

/**
 * 주문 생성 성공 응답
 */
data class WsOrderCreatedResponse(
    override val requestId: String,
    override val success: Boolean = true,
    val orderNumber: Long,
    val message: String = "주문이 접수되었습니다"
) : WsResponse()

/**
 * 주문 조회 응답
 */
data class WsOrderQueriedResponse(
    override val requestId: String,
    override val success: Boolean = true,
    val order: OrderDto
) : WsResponse()

/**
 * 주문 취소 성공 응답
 */
data class WsOrderCancelledResponse(
    override val requestId: String,
    override val success: Boolean = true,
    val orderNumber: Long,
    val message: String = "주문이 취소되었습니다"
) : WsResponse()

/**
 * 포트폴리오 조회 응답
 */
data class WsPortfolioResponse(
    override val requestId: String,
    override val success: Boolean = true,
    val accountNumber: String,
    val holdings: List<StockHoldingDto>
) : WsResponse()

/**
 * 에러 응답 (모든 실패 케이스에 사용)
 */
data class WsErrorResponse(
    override val requestId: String,
    override val success: Boolean = false,
    val errorCode: String,
    val errorMessage: String
) : WsResponse()

// ========== DTO 클래스 ==========

/**
 * Order 도메인을 WebSocket 응답용으로 변환한 DTO
 */
data class OrderDto(
    val orderNumber: Long,
    val stockCode: String,
    val orderType: String,
    val quantity: Int,
    val price: BigDecimal,
    val priceType: String,
    val status: String,
    val orderTime: String
)

/**
 * StockHolding 도메인을 WebSocket 응답용으로 변환한 DTO
 */
data class StockHoldingDto(
    val stockCode: String,
    val quantity: Int,
    val averagePrice: BigDecimal,
    val currentValue: BigDecimal
)

// ========== 도메인 → DTO 변환 확장 함수 ==========

fun Order.toDto(): OrderDto = OrderDto(
    orderNumber = this.orderNumber,
    stockCode = this.stockCode,
    orderType = this.orderType.name,
    quantity = this.quantity,
    price = this.price,
    priceType = this.priceType.name,
    status = this.status.name,
    orderTime = this.orderTime.toString()
)

fun StockHolding.toDto(): StockHoldingDto = StockHoldingDto(
    stockCode = this.stockCode,
    quantity = this.quantity,
    averagePrice = this.averagePrice,
    currentValue = this.averagePrice.multiply(BigDecimal(this.quantity))
)
