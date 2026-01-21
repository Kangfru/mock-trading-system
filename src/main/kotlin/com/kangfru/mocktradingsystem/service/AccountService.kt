package com.kangfru.mocktradingsystem.service

import com.kangfru.mocktradingsystem.domain.Account
import com.kangfru.mocktradingsystem.domain.StockHolding
import com.kangfru.mocktradingsystem.repository.AccountRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val userService: UserService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val accountLocks = ConcurrentHashMap<String, Mutex>()

    private suspend fun <T> withAccountLock(accountNumber: String, block: suspend () -> T): T {
        val mutex = accountLocks.computeIfAbsent(accountNumber) { Mutex() }
        return mutex.withLock {
            block()
        }
    }

    private fun removeLock(accountNumber: String) {
        accountLocks.remove(accountNumber)
    }

    suspend fun createAccount(userId: Long, initialBalance: BigDecimal = BigDecimal.ZERO): Account {
        require(initialBalance >= BigDecimal.ZERO) { "Initial balance cannot be negative" }

        // Verify user exists
        userService.getUser(userId)

        val accountNumber = generateAccountNumber()
        val account = Account(
            accountNumber = accountNumber,
            userId = userId,
            balance = initialBalance,
        )

        val savedAccount = accountRepository.save(account)
        userService.addAccountToUser(userId, accountNumber)

        logger.info("Created account: accountNumber=$accountNumber, userId=$userId, balance=$initialBalance")
        return savedAccount
    }

    suspend fun getAccount(accountNumber: String): Account {
        return accountRepository.findByAccountNumber(accountNumber)
            ?: throw NoSuchElementException("Account not found: accountNumber=$accountNumber")
    }

    suspend fun getAccountsByUserId(userId: Long): List<Account> {
        return accountRepository.findByUserId(userId)
    }

    suspend fun getAllAccounts(): List<Account> {
        return accountRepository.findAll()
    }

    suspend fun deposit(accountNumber: String, amount: BigDecimal): Account = withAccountLock(accountNumber) {
        val account = getAccount(accountNumber)
        val updatedAccount = account.deposit(amount)
        val saved = accountRepository.save(updatedAccount)
        logger.info("Deposited: accountNumber=$accountNumber, amount=$amount, newBalance=${saved.balance}")
        saved
    }

    suspend fun withdraw(accountNumber: String, amount: BigDecimal): Account = withAccountLock(accountNumber) {
        val account = getAccount(accountNumber)
        val updatedAccount = account.withdraw(amount)
        val saved = accountRepository.save(updatedAccount)
        logger.info("Withdrew: accountNumber=$accountNumber, amount=$amount, newBalance=${saved.balance}")
        saved
    }

    suspend fun buyStock(
        accountNumber: String,
        stockCode: String,
        quantity: Int,
        price: BigDecimal,
    ): Account = withAccountLock(accountNumber) {
        require(quantity > 0) { "Quantity must be positive" }
        require(price > BigDecimal.ZERO) { "Price must be positive" }

        val account = getAccount(accountNumber)
        val updatedAccount = account.buyStock(stockCode, quantity, price)
        val saved = accountRepository.save(updatedAccount)

        logger.info(
            "Bought stock: accountNumber=$accountNumber, stockCode=$stockCode, " +
                "quantity=$quantity, price=$price, totalCost=${price.multiply(BigDecimal(quantity))}",
        )
        saved
    }

    suspend fun sellStock(
        accountNumber: String,
        stockCode: String,
        quantity: Int,
        price: BigDecimal,
    ): Account = withAccountLock(accountNumber) {
        require(quantity > 0) { "Quantity must be positive" }
        require(price > BigDecimal.ZERO) { "Price must be positive" }

        val account = getAccount(accountNumber)
        val updatedAccount = account.sellStock(stockCode, quantity, price)
        val saved = accountRepository.save(updatedAccount)

        logger.info(
            "Sold stock: accountNumber=$accountNumber, stockCode=$stockCode, " +
                "quantity=$quantity, price=$price, totalProceeds=${price.multiply(BigDecimal(quantity))}",
        )
        saved
    }

    suspend fun getHoldings(accountNumber: String): Map<String, StockHolding> {
        val account = getAccount(accountNumber)
        return account.holdings
    }

    suspend fun getHolding(accountNumber: String, stockCode: String): StockHolding? {
        val account = getAccount(accountNumber)
        return account.getHolding(stockCode)
    }

    suspend fun deleteAccount(accountNumber: String): Boolean {
        val deleted = withAccountLock(accountNumber) {
            val account = getAccount(accountNumber)
            val result = accountRepository.deleteByAccountNumber(accountNumber)
            if (result) {
                try {
                    userService.removeAccountFromUser(account.userId, accountNumber)
                } catch (e: Exception) {
                    logger.warn("Failed to remove account from user: ${e.message}")
                }
                logger.info("Deleted account: accountNumber=$accountNumber")
            }
            result
        }
        if (deleted) {
            removeLock(accountNumber)
        }
        return deleted
    }

    private suspend fun generateAccountNumber(): String {
        var accountNumber: String
        do {
            accountNumber = "ACC-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"
        } while (accountRepository.existsByAccountNumber(accountNumber))
        return accountNumber
    }
}
