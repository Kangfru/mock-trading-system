package com.kangfru.mocktradingsystem.domain

import java.math.BigDecimal

/**
 * Stock holding information
 */
data class StockHolding(
    val stockCode: String,
    val quantity: Int,
    val averagePrice: BigDecimal,
) {
    val totalValue: BigDecimal
        get() = averagePrice.multiply(BigDecimal(quantity))

    fun addQuantity(
        additionalQuantity: Int,
        purchasePrice: BigDecimal,
    ): StockHolding {
        val totalQuantity = quantity + additionalQuantity
        val totalCost =
            averagePrice
                .multiply(BigDecimal(quantity))
                .add(purchasePrice.multiply(BigDecimal(additionalQuantity)))
        val newAveragePrice = totalCost.divide(BigDecimal(totalQuantity), 2, java.math.RoundingMode.HALF_UP)

        return copy(
            quantity = totalQuantity,
            averagePrice = newAveragePrice,
        )
    }

    fun subtractQuantity(reduceQuantity: Int): StockHolding? {
        if (reduceQuantity > quantity) {
            throw IllegalArgumentException("Cannot reduce quantity more than current holding")
        }

        val remainingQuantity = quantity - reduceQuantity
        return if (remainingQuantity == 0) {
            null
        } else {
            copy(quantity = remainingQuantity)
        }
    }
}
