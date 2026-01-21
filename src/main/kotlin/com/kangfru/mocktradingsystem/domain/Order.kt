package com.kangfru.mocktradingsystem.domain

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class Order(
    val orderId: String = UUID.randomUUID().toString(),
    val orderNumber: Long = 0L,
    val stockCode: String,
    val orderType: OrderType,
    val quantity: Int,
    val price: BigDecimal,
    val priceType: PriceType = PriceType.LIMIT, // 지정가/시장가
    val orderTime: LocalDateTime = LocalDateTime.now(),
    val status: OrderStatus = OrderStatus.PENDING,
    val action: OrderAction = OrderAction.NEW,
    val originalOrderNumber: Long? = null, // 취소/정정 시 원주문 번호
)

enum class OrderType {
    BUY,
    SELL,
}

enum class OrderStatus {
    PENDING, // 대기
    FILLED, // 체결 완료
    CANCELLED, // 취소
    MODIFIED, // 정정됨
}

enum class OrderAction {
    NEW, // 신규 주문
    CANCEL, // 취소
    MODIFY, // 정정
}

enum class PriceType {
    LIMIT, // 지정가
    MARKET, // 시장가
}
