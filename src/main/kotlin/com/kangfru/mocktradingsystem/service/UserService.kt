package com.kangfru.mocktradingsystem.service

import com.kangfru.mocktradingsystem.domain.User
import com.kangfru.mocktradingsystem.repository.UserRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val userLocks = ConcurrentHashMap<Long, Mutex>()

    private suspend fun <T> withUserLock(userId: Long, block: suspend () -> T): T {
        val mutex = userLocks.computeIfAbsent(userId) { Mutex() }
        return mutex.withLock {
            block()
        }
    }

    private fun removeLock(userId: Long) {
        userLocks.remove(userId)
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "***"
        val localPart = parts[0]
        val domain = parts[1]
        val maskedLocal = if (localPart.length <= 2) {
            "***"
        } else {
            localPart.take(2) + "***"
        }
        return "$maskedLocal@$domain"
    }

    suspend fun createUser(username: String, email: String): User {
        require(username.isNotBlank()) { "Username cannot be blank" }
        require(email.isNotBlank()) { "Email cannot be blank" }

        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("User with email $email already exists")
        }

        val user = User(
            seq = 0L,
            username = username,
            email = email,
        )

        val savedUser = userRepository.save(user)
        logger.info("Created user: seq=${savedUser.seq}, username=${savedUser.username}, email=${maskEmail(savedUser.email)}")
        return savedUser
    }

    suspend fun getUser(seq: Long): User {
        return userRepository.findById(seq)
            ?: throw NoSuchElementException("User not found: seq=$seq")
    }

    suspend fun getUserByEmail(email: String): User {
        return userRepository.findByEmail(email)
            ?: throw NoSuchElementException("User not found: email=$email")
    }

    suspend fun getAllUsers(): List<User> {
        return userRepository.findAll()
    }

    suspend fun updateUser(seq: Long, username: String?, email: String?): User = withUserLock(seq) {
        val user = getUser(seq)

        if (email != null && email != user.email) {
            if (userRepository.existsByEmail(email)) {
                throw IllegalArgumentException("Email $email is already in use")
            }
        }

        val updatedUser = user.copy(
            username = username ?: user.username,
            email = email ?: user.email,
            updatedAt = java.time.LocalDateTime.now(),
        )

        val saved = userRepository.save(updatedUser)
        logger.info("Updated user: seq=${saved.seq}, username=${saved.username}")
        saved
    }

    suspend fun deleteUser(seq: Long): Boolean {
        val deleted = userRepository.deleteById(seq)
        if (deleted) {
            logger.info("Deleted user: seq=$seq")
            removeLock(seq)
        }
        return deleted
    }

    suspend fun addAccountToUser(seq: Long, accountNumber: String): User = withUserLock(seq) {
        val user = getUser(seq)
        val updatedUser = user.addAccount(accountNumber)
        val saved = userRepository.save(updatedUser)
        logger.info("Added account $accountNumber to user: seq=$seq")
        saved
    }

    suspend fun removeAccountFromUser(seq: Long, accountNumber: String): User = withUserLock(seq) {
        val user = getUser(seq)
        val updatedUser = user.removeAccount(accountNumber)
        val saved = userRepository.save(updatedUser)
        logger.info("Removed account $accountNumber from user: seq=$seq")
        saved
    }

    suspend fun getUserAccounts(seq: Long): List<String> {
        val user = getUser(seq)
        return user.accountNumbers
    }
}
