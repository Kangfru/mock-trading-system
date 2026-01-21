package com.kangfru.mocktradingsystem.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

data class StockQuote(
    val symbol: String,
    val date: LocalDate,
    val time: LocalTime,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: Long,
)
