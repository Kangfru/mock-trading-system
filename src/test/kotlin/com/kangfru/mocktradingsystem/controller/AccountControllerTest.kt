package com.kangfru.mocktradingsystem.controller

import com.kangfru.mocktradingsystem.repository.AccountRepository
import com.kangfru.mocktradingsystem.repository.UserRepository
import com.kangfru.mocktradingsystem.service.UserService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
class AccountControllerTest {
    @Autowired
    private lateinit var accountController: AccountController

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @BeforeEach
    fun setUp() =
        runTest {
            userRepository.clear()
            accountRepository.clear()
        }

    @Test
    fun `should create account successfully`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")
            val request = CreateAccountRequest(user.seq, BigDecimal("1000.00"))
            val response = accountController.createAccount(request)

            assertEquals(user.seq, response.userId)
            assertEquals(BigDecimal("1000.00"), response.balance)
            assertNotNull(response.accountNumber)
        }

    @Test
    fun `should get account by account number`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")
            val created = accountController.createAccount(CreateAccountRequest(user.seq, BigDecimal("1000.00")))
            val retrieved = accountController.getAccount(created.accountNumber)

            assertEquals(created.accountNumber, retrieved.accountNumber)
            assertEquals(BigDecimal("1000.00"), retrieved.balance)
        }

    @Test
    fun `should get all accounts`() =
        runTest {
            val user1 = userService.createUser("user1", "user1@example.com")
            val user2 = userService.createUser("user2", "user2@example.com")

            accountController.createAccount(CreateAccountRequest(user1.seq, BigDecimal("1000.00")))
            accountController.createAccount(CreateAccountRequest(user2.seq, BigDecimal("2000.00")))

            val accounts = accountController.getAllAccounts()
            assertEquals(2, accounts.size)
        }

    @Test
    fun `should delete account successfully`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")
            val created = accountController.createAccount(CreateAccountRequest(user.seq, BigDecimal("1000.00")))
            accountController.deleteAccount(created.accountNumber)

            val exception = kotlin.runCatching { accountController.getAccount(created.accountNumber) }.exceptionOrNull()
            assertTrue(exception is NoSuchElementException)
        }

    @Test
    fun `should deposit successfully`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")
            val created = accountController.createAccount(CreateAccountRequest(user.seq, BigDecimal("1000.00")))
            val deposited = accountController.deposit(created.accountNumber, DepositRequest(BigDecimal("500.00")))

            assertEquals(BigDecimal("1500.00"), deposited.balance)
        }

    @Test
    fun `should withdraw successfully`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")
            val created = accountController.createAccount(CreateAccountRequest(user.seq, BigDecimal("1000.00")))
            val withdrawn = accountController.withdraw(created.accountNumber, WithdrawRequest(BigDecimal("300.00")))

            assertEquals(BigDecimal("700.00"), withdrawn.balance)
        }

    @Test
    fun `should buy stock successfully`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")
            val created = accountController.createAccount(CreateAccountRequest(user.seq, BigDecimal("10000.00")))
            val bought =
                accountController.buyStock(
                    created.accountNumber,
                    BuyStockRequest("AAPL", 10, BigDecimal("150.00")),
                )

            assertEquals(BigDecimal("8500.00"), bought.balance)
            assertEquals(10, bought.holdings["AAPL"]?.quantity)
            assertEquals(BigDecimal("150.00"), bought.holdings["AAPL"]?.averagePrice)
        }

    @Test
    fun `should sell stock successfully`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")
            val created = accountController.createAccount(CreateAccountRequest(user.seq, BigDecimal("10000.00")))

            accountController.buyStock(created.accountNumber, BuyStockRequest("AAPL", 10, BigDecimal("150.00")))
            val sold =
                accountController.sellStock(
                    created.accountNumber,
                    SellStockRequest("AAPL", 5, BigDecimal("160.00")),
                )

            assertEquals(BigDecimal("9300.00"), sold.balance)
            assertEquals(5, sold.holdings["AAPL"]?.quantity)
        }

    @Test
    fun `should get holdings successfully`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")
            val created = accountController.createAccount(CreateAccountRequest(user.seq, BigDecimal("10000.00")))

            accountController.buyStock(created.accountNumber, BuyStockRequest("AAPL", 10, BigDecimal("150.00")))
            accountController.buyStock(created.accountNumber, BuyStockRequest("GOOGL", 5, BigDecimal("200.00")))

            val holdings = accountController.getHoldings(created.accountNumber)
            assertEquals(2, holdings.size)
            assertEquals(10, holdings["AAPL"]?.quantity)
            assertEquals(5, holdings["GOOGL"]?.quantity)
        }

    @Test
    fun `should get specific holding successfully`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")
            val created = accountController.createAccount(CreateAccountRequest(user.seq, BigDecimal("10000.00")))

            accountController.buyStock(created.accountNumber, BuyStockRequest("AAPL", 10, BigDecimal("150.00")))

            val holding = accountController.getHolding(created.accountNumber, "AAPL")
            assertNotNull(holding)
            assertEquals("AAPL", holding.stockCode)
            assertEquals(10, holding.quantity)
            assertEquals(BigDecimal("150.00"), holding.averagePrice)
        }

    @Test
    fun `should return null for non-existent holding`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")
            val created = accountController.createAccount(CreateAccountRequest(user.seq, BigDecimal("10000.00")))

            val holding = accountController.getHolding(created.accountNumber, "AAPL")
            assertNull(holding)
        }
}
