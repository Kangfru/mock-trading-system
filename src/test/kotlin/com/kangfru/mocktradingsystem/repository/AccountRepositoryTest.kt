package com.kangfru.mocktradingsystem.repository

import com.kangfru.mocktradingsystem.domain.Account
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountRepositoryTest {
    private lateinit var repository: AccountRepository

    @BeforeEach
    fun setUp() {
        repository = AccountRepository()
    }

    @Test
    fun `should save account successfully`() =
        runTest {
            val account = Account("ACC-001", 1L, BigDecimal("1000.00"))
            val saved = repository.save(account)

            assertEquals("ACC-001", saved.accountNumber)
            assertEquals(1L, saved.userId)
            assertEquals(BigDecimal("1000.00"), saved.balance)
        }

    @Test
    fun `should find account by account number`() =
        runTest {
            val account = Account("ACC-001", 1L, BigDecimal("1000.00"))
            repository.save(account)
            val found = repository.findByAccountNumber("ACC-001")

            assertEquals("ACC-001", found?.accountNumber)
            assertEquals(1L, found?.userId)
        }

    @Test
    fun `should return null when account not found by account number`() =
        runTest {
            val found = repository.findByAccountNumber("INVALID")
            assertNull(found)
        }

    @Test
    fun `should find accounts by user id`() =
        runTest {
            repository.save(Account("ACC-001", 1L, BigDecimal("1000.00")))
            repository.save(Account("ACC-002", 1L, BigDecimal("2000.00")))
            repository.save(Account("ACC-003", 2L, BigDecimal("3000.00")))

            val accounts = repository.findByUserId(1L)
            assertEquals(2, accounts.size)
            assertTrue(accounts.any { it.accountNumber == "ACC-001" })
            assertTrue(accounts.any { it.accountNumber == "ACC-002" })
        }

    @Test
    fun `should return empty list when no accounts found for user id`() =
        runTest {
            val accounts = repository.findByUserId(999L)
            assertTrue(accounts.isEmpty())
        }

    @Test
    fun `should find all accounts`() =
        runTest {
            repository.save(Account("ACC-001", 1L, BigDecimal("1000.00")))
            repository.save(Account("ACC-002", 2L, BigDecimal("2000.00")))

            val accounts = repository.findAll()
            assertEquals(2, accounts.size)
        }

    @Test
    fun `should check if account exists by account number`() =
        runTest {
            repository.save(Account("ACC-001", 1L, BigDecimal("1000.00")))

            assertTrue(repository.existsByAccountNumber("ACC-001"))
            assertFalse(repository.existsByAccountNumber("INVALID"))
        }

    @Test
    fun `should delete account by account number`() =
        runTest {
            val account = Account("ACC-001", 1L, BigDecimal("1000.00"))
            repository.save(account)
            val deleted = repository.deleteByAccountNumber("ACC-001")

            assertTrue(deleted)
            assertNull(repository.findByAccountNumber("ACC-001"))
            assertTrue(repository.findByUserId(1L).isEmpty())
        }

    @Test
    fun `should return false when deleting non-existent account`() =
        runTest {
            val deleted = repository.deleteByAccountNumber("INVALID")
            assertFalse(deleted)
        }

    @Test
    fun `should count accounts correctly`() =
        runTest {
            assertEquals(0, repository.count())

            repository.save(Account("ACC-001", 1L, BigDecimal("1000.00")))
            assertEquals(1, repository.count())

            repository.save(Account("ACC-002", 2L, BigDecimal("2000.00")))
            assertEquals(2, repository.count())
        }

    @Test
    fun `should clear all accounts`() =
        runTest {
            repository.save(Account("ACC-001", 1L, BigDecimal("1000.00")))
            repository.save(Account("ACC-002", 2L, BigDecimal("2000.00")))
            assertEquals(2, repository.count())

            repository.clear()
            assertEquals(0, repository.count())
            assertNull(repository.findByAccountNumber("ACC-001"))
            assertTrue(repository.findByUserId(1L).isEmpty())
        }

    @Test
    fun `should update account correctly`() =
        runTest {
            val account = Account("ACC-001", 1L, BigDecimal("1000.00"))
            repository.save(account)
            val updated = account.copy(balance = BigDecimal("2000.00"))
            repository.save(updated)

            val found = repository.findByAccountNumber("ACC-001")
            assertEquals(BigDecimal("2000.00"), found?.balance)
        }

    @Test
    fun `should maintain user accounts index correctly`() =
        runTest {
            repository.save(Account("ACC-001", 1L, BigDecimal("1000.00")))
            repository.save(Account("ACC-002", 1L, BigDecimal("2000.00")))
            repository.save(Account("ACC-003", 1L, BigDecimal("3000.00")))

            assertEquals(3, repository.findByUserId(1L).size)

            repository.deleteByAccountNumber("ACC-002")
            assertEquals(2, repository.findByUserId(1L).size)
            assertFalse(repository.findByUserId(1L).any { it.accountNumber == "ACC-002" })
        }

    @Test
    fun `should handle multiple users with multiple accounts`() =
        runTest {
            repository.save(Account("ACC-001", 1L, BigDecimal("1000.00")))
            repository.save(Account("ACC-002", 1L, BigDecimal("2000.00")))
            repository.save(Account("ACC-003", 2L, BigDecimal("3000.00")))
            repository.save(Account("ACC-004", 2L, BigDecimal("4000.00")))
            repository.save(Account("ACC-005", 3L, BigDecimal("5000.00")))

            assertEquals(2, repository.findByUserId(1L).size)
            assertEquals(2, repository.findByUserId(2L).size)
            assertEquals(1, repository.findByUserId(3L).size)
            assertEquals(5, repository.count())
        }
}
