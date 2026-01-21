package com.kangfru.mocktradingsystem.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountTest {
    @Test
    fun `should calculate total assets correctly`() {
        val holdings =
            mapOf(
                "AAPL" to StockHolding("AAPL", 10, BigDecimal("100.00")),
                "GOOGL" to StockHolding("GOOGL", 5, BigDecimal("200.00")),
            )
        val account = Account("ACC-001", 1L, BigDecimal("500.00"), holdings)

        val expected =
            BigDecimal("500.00")
                .add(BigDecimal("1000.00"))
                .add(BigDecimal("1000.00"))
        assertEquals(expected, account.totalAssets)
    }

    @Test
    fun `should deposit successfully`() {
        val account = Account("ACC-001", 1L, BigDecimal("100.00"))
        val updated = account.deposit(BigDecimal("50.00"))

        assertEquals(BigDecimal("150.00"), updated.balance)
    }

    @Test
    fun `should throw exception when depositing negative amount`() {
        val account = Account("ACC-001", 1L, BigDecimal("100.00"))

        assertThrows<IllegalArgumentException> {
            account.deposit(BigDecimal("-50.00"))
        }
    }

    @Test
    fun `should withdraw successfully`() {
        val account = Account("ACC-001", 1L, BigDecimal("100.00"))
        val updated = account.withdraw(BigDecimal("30.00"))

        assertEquals(BigDecimal("70.00"), updated.balance)
    }

    @Test
    fun `should throw exception when withdrawing more than balance`() {
        val account = Account("ACC-001", 1L, BigDecimal("100.00"))

        assertThrows<IllegalArgumentException> {
            account.withdraw(BigDecimal("150.00"))
        }
    }

    @Test
    fun `should buy stock successfully for new holding`() {
        val account = Account("ACC-001", 1L, BigDecimal("1000.00"))
        val updated = account.buyStock("AAPL", 10, BigDecimal("50.00"))

        assertEquals(BigDecimal("500.00"), updated.balance)
        assertEquals(10, updated.holdings["AAPL"]?.quantity)
        assertEquals(BigDecimal("50.00"), updated.holdings["AAPL"]?.averagePrice)
    }

    @Test
    fun `should buy stock successfully for existing holding`() {
        val existingHolding = StockHolding("AAPL", 10, BigDecimal("100.00"))
        val account = Account("ACC-001", 1L, BigDecimal("2000.00"), mapOf("AAPL" to existingHolding))
        val updated = account.buyStock("AAPL", 10, BigDecimal("120.00"))

        assertEquals(BigDecimal("800.00"), updated.balance)
        assertEquals(20, updated.holdings["AAPL"]?.quantity)
        assertEquals(BigDecimal("110.00"), updated.holdings["AAPL"]?.averagePrice)
    }

    @Test
    fun `should throw exception when buying stock with insufficient balance`() {
        val account = Account("ACC-001", 1L, BigDecimal("100.00"))

        assertThrows<IllegalArgumentException> {
            account.buyStock("AAPL", 10, BigDecimal("50.00"))
        }
    }

    @Test
    fun `should sell stock successfully`() {
        val holding = StockHolding("AAPL", 10, BigDecimal("100.00"))
        val account = Account("ACC-001", 1L, BigDecimal("500.00"), mapOf("AAPL" to holding))
        val updated = account.sellStock("AAPL", 5, BigDecimal("120.00"))

        assertEquals(BigDecimal("1100.00"), updated.balance)
        assertEquals(5, updated.holdings["AAPL"]?.quantity)
    }

    @Test
    fun `should sell all stock and remove from holdings`() {
        val holding = StockHolding("AAPL", 10, BigDecimal("100.00"))
        val account = Account("ACC-001", 1L, BigDecimal("500.00"), mapOf("AAPL" to holding))
        val updated = account.sellStock("AAPL", 10, BigDecimal("120.00"))

        assertEquals(BigDecimal("1700.00"), updated.balance)
        assertNull(updated.holdings["AAPL"])
    }

    @Test
    fun `should throw exception when selling non-existent stock`() {
        val account = Account("ACC-001", 1L, BigDecimal("500.00"))

        assertThrows<IllegalArgumentException> {
            account.sellStock("AAPL", 5, BigDecimal("120.00"))
        }
    }

    @Test
    fun `should throw exception when selling more than owned`() {
        val holding = StockHolding("AAPL", 10, BigDecimal("100.00"))
        val account = Account("ACC-001", 1L, BigDecimal("500.00"), mapOf("AAPL" to holding))

        assertThrows<IllegalArgumentException> {
            account.sellStock("AAPL", 15, BigDecimal("120.00"))
        }
    }

    @Test
    fun `should check if account has sufficient stock`() {
        val holding = StockHolding("AAPL", 10, BigDecimal("100.00"))
        val account = Account("ACC-001", 1L, BigDecimal("500.00"), mapOf("AAPL" to holding))

        assertTrue(account.hasStock("AAPL", 5))
        assertTrue(account.hasStock("AAPL", 10))
        assertFalse(account.hasStock("AAPL", 15))
        assertFalse(account.hasStock("GOOGL", 1))
    }

    @Test
    fun `should get holding for specific stock`() {
        val holding = StockHolding("AAPL", 10, BigDecimal("100.00"))
        val account = Account("ACC-001", 1L, BigDecimal("500.00"), mapOf("AAPL" to holding))

        assertEquals(holding, account.getHolding("AAPL"))
        assertNull(account.getHolding("GOOGL"))
    }
}
