package com.kangfru.mocktradingsystem.repository

import com.kangfru.mocktradingsystem.domain.Account
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Repository

@Repository
class AccountRepository {
    private val accounts = mutableMapOf<String, Account>()
    private val userAccountsIndex = mutableMapOf<Long, MutableSet<String>>()
    private val mutex = Mutex()

    suspend fun save(account: Account): Account = mutex.withLock {
        accounts[account.accountNumber] = account

        val existingSet = userAccountsIndex[account.userId] ?: mutableSetOf()
        existingSet.add(account.accountNumber)
        userAccountsIndex[account.userId] = existingSet

        return account
    }

    suspend fun findByAccountNumber(accountNumber: String): Account? = mutex.withLock {
        accounts[accountNumber]
    }

    suspend fun findByUserId(userId: Long): List<Account> = mutex.withLock {
        val accountNumbers = userAccountsIndex[userId] ?: return emptyList()
        accountNumbers.mapNotNull { accounts[it] }
    }

    suspend fun findAll(): List<Account> = mutex.withLock {
        accounts.values.toList()
    }

    suspend fun existsByAccountNumber(accountNumber: String): Boolean = mutex.withLock {
        accounts.containsKey(accountNumber)
    }

    suspend fun deleteByAccountNumber(accountNumber: String): Boolean = mutex.withLock {
        val account = accounts.remove(accountNumber)
        if (account != null) {
            userAccountsIndex[account.userId]?.remove(accountNumber)
            return true
        }
        return false
    }

    suspend fun count(): Int = mutex.withLock {
        accounts.size
    }

    suspend fun clear() = mutex.withLock {
        accounts.clear()
        userAccountsIndex.clear()
    }
}
