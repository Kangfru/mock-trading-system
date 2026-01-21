package com.kangfru.mocktradingsystem.domain

import java.math.BigDecimal

data class CreateOrderRequest(
    val stockCode: String,
    val orderType: OrderType,
    val quantity: Int,
    val price: BigDecimal? = null, // 시장가 주문 시 null
    val priceType: PriceType = PriceType.LIMIT,
)

data class CancelOrderRequest(
    val originalOrderNumber: Long,
)

data class ModifyOrderRequest(
    val originalOrderNumber: Long,
    val newQuantity: Int? = null,
    val newPrice: BigDecimal? = null,
)
