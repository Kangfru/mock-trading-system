package com.kangfru.mocktradingsystem.domain

import java.time.LocalDateTime

/**
 * User entity
 */
data class User(
    val seq: Long,
    val username: String,
    val email: String,
    val accountNumbers: List<String> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun addAccount(accountNumber: String): User {
        require(accountNumber.isNotBlank()) { "Account number cannot be blank" }
        require(!accountNumbers.contains(accountNumber)) { "Account already exists" }
        return copy(
            accountNumbers = accountNumbers + accountNumber,
            updatedAt = LocalDateTime.now(),
        )
    }

    fun removeAccount(accountNumber: String): User {
        require(accountNumbers.contains(accountNumber)) { "Account does not exist" }
        return copy(
            accountNumbers = accountNumbers - accountNumber,
            updatedAt = LocalDateTime.now(),
        )
    }
}
