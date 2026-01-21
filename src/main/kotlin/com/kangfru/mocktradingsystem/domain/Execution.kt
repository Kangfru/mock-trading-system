package com.kangfru.mocktradingsystem.domain

import java.math.BigDecimal
import java.time.LocalDateTime

data class Execution(
    val executionId: Long,
    val orderNumber: Long,
    val orderId: String,
    val stockCode: String,
    val orderType: OrderType,
    val executedQuantity: Int,
    val executedPrice: BigDecimal,
    val executionTime: LocalDateTime = LocalDateTime.now(),
)
