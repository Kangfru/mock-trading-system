package com.kangfru.mocktradingsystem.domain

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Account entity
 */
data class Account(
    val accountNumber: String,
    val userId: Long,
    val balance: BigDecimal = BigDecimal.ZERO,
    val holdings: Map<String, StockHolding> = emptyMap(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    val totalAssets: BigDecimal
        get() = balance.add(holdings.values.fold(BigDecimal.ZERO) { acc, holding -> acc.add(holding.totalValue) })

    fun deposit(amount: BigDecimal): Account {
        require(amount > BigDecimal.ZERO) { "Deposit amount must be positive" }
        return copy(
            balance = balance.add(amount),
            updatedAt = LocalDateTime.now(),
        )
    }

    fun withdraw(amount: BigDecimal): Account {
        require(amount > BigDecimal.ZERO) { "Withdraw amount must be positive" }
        require(balance >= amount) { "Insufficient balance" }
        return copy(
            balance = balance.subtract(amount),
            updatedAt = LocalDateTime.now(),
        )
    }

    fun buyStock(
        stockCode: String,
        quantity: Int,
        price: BigDecimal,
    ): Account {
        val totalCost = price.multiply(BigDecimal(quantity))
        require(balance >= totalCost) { "Insufficient balance for purchase" }

        val currentHolding = holdings[stockCode]
        val newHolding =
            if (currentHolding == null) {
                StockHolding(stockCode, quantity, price)
            } else {
                currentHolding.addQuantity(quantity, price)
            }

        return copy(
            balance = balance.subtract(totalCost),
            holdings = holdings + (stockCode to newHolding),
            updatedAt = LocalDateTime.now(),
        )
    }

    fun sellStock(
        stockCode: String,
        quantity: Int,
        price: BigDecimal,
    ): Account {
        val currentHolding =
            holdings[stockCode]
                ?: throw IllegalArgumentException("No holding found for stock: $stockCode")
        require(currentHolding.quantity >= quantity) { "Insufficient stock quantity" }

        val totalProceeds = price.multiply(BigDecimal(quantity))
        val newHolding = currentHolding.subtractQuantity(quantity)

        val newHoldings =
            if (newHolding == null) {
                holdings - stockCode
            } else {
                holdings + (stockCode to newHolding)
            }

        return copy(
            balance = balance.add(totalProceeds),
            holdings = newHoldings,
            updatedAt = LocalDateTime.now(),
        )
    }

    fun getHolding(stockCode: String): StockHolding? = holdings[stockCode]

    fun hasStock(
        stockCode: String,
        quantity: Int,
    ): Boolean {
        val holding = holdings[stockCode] ?: return false
        return holding.quantity >= quantity
    }
}
