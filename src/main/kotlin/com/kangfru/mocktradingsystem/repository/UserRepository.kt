package com.kangfru.mocktradingsystem.repository

import com.kangfru.mocktradingsystem.domain.User
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Repository

@Repository
class UserRepository {
    private val users = mutableMapOf<Long, User>()
    private val emailIndex = mutableMapOf<String, Long>()
    private var sequenceGenerator = 1L
    private val mutex = Mutex()

    suspend fun save(user: User): User = mutex.withLock {
        val savedUser = if (user.seq == 0L) {
            user.copy(seq = sequenceGenerator++)
        } else {
            user
        }

        // Remove old email index if email has changed
        val existingUser = users[savedUser.seq]
        if (existingUser != null && existingUser.email != savedUser.email) {
            emailIndex.remove(existingUser.email)
        }

        users[savedUser.seq] = savedUser
        emailIndex[savedUser.email] = savedUser.seq
        return savedUser
    }

    suspend fun findById(seq: Long): User? = mutex.withLock {
        users[seq]
    }

    suspend fun findByEmail(email: String): User? = mutex.withLock {
        val seq = emailIndex[email] ?: return null
        users[seq]
    }

    suspend fun findAll(): List<User> = mutex.withLock {
        users.values.toList()
    }

    suspend fun existsByEmail(email: String): Boolean = mutex.withLock {
        emailIndex.containsKey(email)
    }

    suspend fun deleteById(seq: Long): Boolean = mutex.withLock {
        val user = users.remove(seq)
        if (user != null) {
            emailIndex.remove(user.email)
            return true
        }
        return false
    }

    suspend fun count(): Int = mutex.withLock {
        users.size
    }

    suspend fun clear() = mutex.withLock {
        users.clear()
        emailIndex.clear()
        sequenceGenerator = 1L
    }
}
