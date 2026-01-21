package com.kangfru.mocktradingsystem.service

import com.kangfru.mocktradingsystem.repository.AccountRepository
import com.kangfru.mocktradingsystem.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountServiceTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var userRepository: UserRepository
    private lateinit var userService: UserService
    private lateinit var accountService: AccountService

    @BeforeEach
    fun setUp() {
        accountRepository = AccountRepository()
        userRepository = UserRepository()
        userService = UserService(userRepository)
        accountService = AccountService(accountRepository, userService)
    }

    @Test
    fun `should create account successfully`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("1000.00"))

        assertNotNull(account.accountNumber)
        assertEquals(user.seq, account.userId)
        assertEquals(BigDecimal("1000.00"), account.balance)
    }

    @Test
    fun `should create account with zero balance by default`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq)

        assertEquals(BigDecimal.ZERO, account.balance)
    }

    @Test
    fun `should throw exception when creating account with negative balance`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")

        assertThrows<IllegalArgumentException> {
            accountService.createAccount(user.seq, BigDecimal("-100.00"))
        }
    }

    @Test
    fun `should throw exception when creating account for non-existent user`() = runTest {
        assertThrows<NoSuchElementException> {
            accountService.createAccount(999L)
        }
    }

    @Test
    fun `should get account by account number`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val created = accountService.createAccount(user.seq, BigDecimal("1000.00"))
        val retrieved = accountService.getAccount(created.accountNumber)

        assertEquals(created.accountNumber, retrieved.accountNumber)
        assertEquals(created.balance, retrieved.balance)
    }

    @Test
    fun `should throw exception when getting non-existent account`() = runTest {
        assertThrows<NoSuchElementException> {
            accountService.getAccount("INVALID-ACCOUNT")
        }
    }

    @Test
    fun `should get accounts by user id`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        accountService.createAccount(user.seq, BigDecimal("1000.00"))
        accountService.createAccount(user.seq, BigDecimal("2000.00"))

        val accounts = accountService.getAccountsByUserId(user.seq)
        assertEquals(2, accounts.size)
    }

    @Test
    fun `should get all accounts`() = runTest {
        val user1 = userService.createUser("user1", "user1@example.com")
        val user2 = userService.createUser("user2", "user2@example.com")
        accountService.createAccount(user1.seq)
        accountService.createAccount(user2.seq)

        val accounts = accountService.getAllAccounts()
        assertEquals(2, accounts.size)
    }

    @Test
    fun `should deposit successfully`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("1000.00"))
        val updated = accountService.deposit(account.accountNumber, BigDecimal("500.00"))

        assertEquals(BigDecimal("1500.00"), updated.balance)
    }

    @Test
    fun `should throw exception when depositing negative amount`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("1000.00"))

        assertThrows<IllegalArgumentException> {
            accountService.deposit(account.accountNumber, BigDecimal("-500.00"))
        }
    }

    @Test
    fun `should withdraw successfully`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("1000.00"))
        val updated = accountService.withdraw(account.accountNumber, BigDecimal("300.00"))

        assertEquals(BigDecimal("700.00"), updated.balance)
    }

    @Test
    fun `should throw exception when withdrawing more than balance`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("1000.00"))

        assertThrows<IllegalArgumentException> {
            accountService.withdraw(account.accountNumber, BigDecimal("1500.00"))
        }
    }

    @Test
    fun `should buy stock successfully`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("10000.00"))
        val updated = accountService.buyStock(account.accountNumber, "AAPL", 10, BigDecimal("150.00"))

        assertEquals(BigDecimal("8500.00"), updated.balance)
        assertEquals(10, updated.holdings["AAPL"]?.quantity)
        assertEquals(BigDecimal("150.00"), updated.holdings["AAPL"]?.averagePrice)
    }

    @Test
    fun `should throw exception when buying stock with insufficient balance`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("100.00"))

        assertThrows<IllegalArgumentException> {
            accountService.buyStock(account.accountNumber, "AAPL", 10, BigDecimal("150.00"))
        }
    }

    @Test
    fun `should throw exception when buying stock with invalid quantity`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("10000.00"))

        assertThrows<IllegalArgumentException> {
            accountService.buyStock(account.accountNumber, "AAPL", 0, BigDecimal("150.00"))
        }
    }

    @Test
    fun `should throw exception when buying stock with invalid price`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("10000.00"))

        assertThrows<IllegalArgumentException> {
            accountService.buyStock(account.accountNumber, "AAPL", 10, BigDecimal.ZERO)
        }
    }

    @Test
    fun `should sell stock successfully`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("10000.00"))
        accountService.buyStock(account.accountNumber, "AAPL", 10, BigDecimal("150.00"))
        val updated = accountService.sellStock(account.accountNumber, "AAPL", 5, BigDecimal("160.00"))

        assertEquals(BigDecimal("9300.00"), updated.balance)
        assertEquals(5, updated.holdings["AAPL"]?.quantity)
    }

    @Test
    fun `should throw exception when selling non-existent stock`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("10000.00"))

        assertThrows<IllegalArgumentException> {
            accountService.sellStock(account.accountNumber, "AAPL", 5, BigDecimal("160.00"))
        }
    }

    @Test
    fun `should throw exception when selling more than owned`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("10000.00"))
        accountService.buyStock(account.accountNumber, "AAPL", 10, BigDecimal("150.00"))

        assertThrows<IllegalArgumentException> {
            accountService.sellStock(account.accountNumber, "AAPL", 15, BigDecimal("160.00"))
        }
    }

    @Test
    fun `should throw exception when selling stock with invalid quantity`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("10000.00"))
        accountService.buyStock(account.accountNumber, "AAPL", 10, BigDecimal("150.00"))

        assertThrows<IllegalArgumentException> {
            accountService.sellStock(account.accountNumber, "AAPL", 0, BigDecimal("160.00"))
        }
    }

    @Test
    fun `should get holdings successfully`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("10000.00"))
        accountService.buyStock(account.accountNumber, "AAPL", 10, BigDecimal("150.00"))
        accountService.buyStock(account.accountNumber, "GOOGL", 5, BigDecimal("200.00"))

        val holdings = accountService.getHoldings(account.accountNumber)
        assertEquals(2, holdings.size)
        assertNotNull(holdings["AAPL"])
        assertNotNull(holdings["GOOGL"])
    }

    @Test
    fun `should get specific holding successfully`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("10000.00"))
        accountService.buyStock(account.accountNumber, "AAPL", 10, BigDecimal("150.00"))

        val holding = accountService.getHolding(account.accountNumber, "AAPL")
        assertNotNull(holding)
        assertEquals(10, holding.quantity)
    }

    @Test
    fun `should return null for non-existent holding`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("10000.00"))

        val holding = accountService.getHolding(account.accountNumber, "AAPL")
        assertNull(holding)
    }

    @Test
    fun `should delete account successfully`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account = accountService.createAccount(user.seq, BigDecimal("10000.00"))
        val deleted = accountService.deleteAccount(account.accountNumber)

        assertTrue(deleted)
        assertThrows<NoSuchElementException> {
            accountService.getAccount(account.accountNumber)
        }
    }

    @Test
    fun `should generate unique account numbers`() = runTest {
        val user = userService.createUser("testuser", "test@example.com")
        val account1 = accountService.createAccount(user.seq)
        val account2 = accountService.createAccount(user.seq)

        assertTrue(account1.accountNumber != account2.accountNumber)
    }

    @Nested
    inner class ConcurrencyTests {

        @Test
        fun `should handle concurrent deposits correctly`() = runTest {
            // Given
            val user = userService.createUser("testuser", "test@example.com")
            val account = accountService.createAccount(user.seq, BigDecimal("1000.00"))
            val depositAmount = BigDecimal("100.00")
            val concurrentOperations = 10

            // When: 10 concurrent deposits of 100 each
            val jobs = List(concurrentOperations) {
                async {
                    accountService.deposit(account.accountNumber, depositAmount)
                }
            }
            jobs.awaitAll()

            // Then: balance should be 1000 + (100 * 10) = 2000
            val finalAccount = accountService.getAccount(account.accountNumber)
            assertEquals(BigDecimal("2000.00"), finalAccount.balance)
        }

        @Test
        fun `should handle concurrent withdrawals correctly`() = runTest {
            // Given
            val user = userService.createUser("testuser", "test@example.com")
            val account = accountService.createAccount(user.seq, BigDecimal("2000.00"))
            val withdrawAmount = BigDecimal("100.00")
            val concurrentOperations = 10

            // When: 10 concurrent withdrawals of 100 each
            val jobs = List(concurrentOperations) {
                async {
                    accountService.withdraw(account.accountNumber, withdrawAmount)
                }
            }
            jobs.awaitAll()

            // Then: balance should be 2000 - (100 * 10) = 1000
            val finalAccount = accountService.getAccount(account.accountNumber)
            assertEquals(BigDecimal("1000.00"), finalAccount.balance)
        }

        @Test
        fun `should handle concurrent deposits and withdrawals correctly`() = runTest {
            // Given
            val user = userService.createUser("testuser", "test@example.com")
            val account = accountService.createAccount(user.seq, BigDecimal("1000.00"))
            val amount = BigDecimal("50.00")
            val operationsPerType = 10

            // When: 10 deposits and 10 withdrawals concurrently
            val deposits = List(operationsPerType) {
                async {
                    accountService.deposit(account.accountNumber, amount)
                }
            }
            val withdrawals = List(operationsPerType) {
                async {
                    accountService.withdraw(account.accountNumber, amount)
                }
            }
            (deposits + withdrawals).awaitAll()

            // Then: balance should remain 1000 (10 * 50 deposits - 10 * 50 withdrawals = 0 net change)
            val finalAccount = accountService.getAccount(account.accountNumber)
            assertEquals(BigDecimal("1000.00"), finalAccount.balance)
        }

        @Test
        fun `should handle concurrent stock purchases correctly`() = runTest {
            // Given
            val user = userService.createUser("testuser", "test@example.com")
            val account = accountService.createAccount(user.seq, BigDecimal("10000.00"))
            val stockCode = "AAPL"
            val quantity = 1
            val price = BigDecimal("100.00")
            val concurrentOperations = 10

            // When: 10 concurrent stock purchases
            val jobs = List(concurrentOperations) {
                async {
                    accountService.buyStock(account.accountNumber, stockCode, quantity, price)
                }
            }
            jobs.awaitAll()

            // Then: balance should be 10000 - (1 * 100 * 10) = 9000, holding should be 10
            val finalAccount = accountService.getAccount(account.accountNumber)
            assertEquals(BigDecimal("9000.00"), finalAccount.balance)
            assertEquals(10, finalAccount.holdings[stockCode]?.quantity)
        }

        @Test
        fun `should handle concurrent stock sales correctly`() = runTest {
            // Given
            val user = userService.createUser("testuser", "test@example.com")
            val account = accountService.createAccount(user.seq, BigDecimal("10000.00"))
            val stockCode = "AAPL"

            // Buy 20 stocks first
            accountService.buyStock(account.accountNumber, stockCode, 20, BigDecimal("100.00"))

            val sellQuantity = 1
            val sellPrice = BigDecimal("150.00")
            val concurrentOperations = 10

            // When: 10 concurrent stock sales of 1 each
            val jobs = List(concurrentOperations) {
                async {
                    accountService.sellStock(account.accountNumber, stockCode, sellQuantity, sellPrice)
                }
            }
            jobs.awaitAll()

            // Then: balance should be 8000 (after buying 20) + (1 * 150 * 10) = 9500
            // Holding should be 20 - 10 = 10
            val finalAccount = accountService.getAccount(account.accountNumber)
            assertEquals(BigDecimal("9500.00"), finalAccount.balance)
            assertEquals(10, finalAccount.holdings[stockCode]?.quantity)
        }

        @Test
        fun `should handle concurrent operations on different accounts independently`() = runTest {
            // Given
            val user = userService.createUser("testuser", "test@example.com")
            val account1 = accountService.createAccount(user.seq, BigDecimal("1000.00"))
            val account2 = accountService.createAccount(user.seq, BigDecimal("1000.00"))
            val amount = BigDecimal("100.00")
            val operationsPerAccount = 5

            // When: concurrent operations on both accounts
            val account1Operations = List(operationsPerAccount) {
                async {
                    accountService.deposit(account1.accountNumber, amount)
                }
            }
            val account2Operations = List(operationsPerAccount) {
                async {
                    accountService.withdraw(account2.accountNumber, amount)
                }
            }
            (account1Operations + account2Operations).awaitAll()

            // Then: each account should have correct independent balances
            val finalAccount1 = accountService.getAccount(account1.accountNumber)
            val finalAccount2 = accountService.getAccount(account2.accountNumber)
            assertEquals(BigDecimal("1500.00"), finalAccount1.balance)
            assertEquals(BigDecimal("500.00"), finalAccount2.balance)
        }
    }
}
