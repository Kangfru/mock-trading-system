package com.kangfru.mocktradingsystem.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StockHoldingTest {

    @Test
    fun `should calculate total value correctly`() {
        val holding = StockHolding("AAPL", 10, BigDecimal("150.00"))
        assertEquals(BigDecimal("1500.00"), holding.totalValue)
    }

    @Test
    fun `should add quantity and recalculate average price`() {
        val holding = StockHolding("AAPL", 10, BigDecimal("100.00"))
        val updated = holding.addQuantity(10, BigDecimal("120.00"))

        assertEquals(20, updated.quantity)
        assertEquals(BigDecimal("110.00"), updated.averagePrice)
    }

    @Test
    fun `should subtract quantity correctly`() {
        val holding = StockHolding("AAPL", 10, BigDecimal("100.00"))
        val updated = holding.subtractQuantity(5)

        assertEquals(5, updated?.quantity)
        assertEquals(BigDecimal("100.00"), updated?.averagePrice)
    }

    @Test
    fun `should return null when subtracting all quantity`() {
        val holding = StockHolding("AAPL", 10, BigDecimal("100.00"))
        val updated = holding.subtractQuantity(10)

        assertNull(updated)
    }

    @Test
    fun `should throw exception when subtracting more than available`() {
        val holding = StockHolding("AAPL", 10, BigDecimal("100.00"))

        assertThrows<IllegalArgumentException> {
            holding.subtractQuantity(15)
        }
    }
}
