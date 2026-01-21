package com.kangfru.mocktradingsystem.repository

import com.kangfru.mocktradingsystem.domain.User
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserRepositoryTest {
    private lateinit var repository: UserRepository

    @BeforeEach
    fun setUp() {
        repository = UserRepository()
    }

    @Test
    fun `should save user with auto-generated seq`() =
        runTest {
            val user = User(0L, "testuser", "test@example.com")
            val saved = repository.save(user)

            assertTrue(saved.seq > 0)
            assertEquals("testuser", saved.username)
        }

    @Test
    fun `should save user with existing seq`() =
        runTest {
            val user = User(100L, "testuser", "test@example.com")
            val saved = repository.save(user)

            assertEquals(100L, saved.seq)
        }

    @Test
    fun `should find user by id`() =
        runTest {
            val user = User(0L, "testuser", "test@example.com")
            val saved = repository.save(user)
            val found = repository.findById(saved.seq)

            assertEquals(saved.seq, found?.seq)
            assertEquals(saved.username, found?.username)
        }

    @Test
    fun `should return null when user not found by id`() =
        runTest {
            val found = repository.findById(999L)
            assertNull(found)
        }

    @Test
    fun `should find user by email`() =
        runTest {
            val user = User(0L, "testuser", "test@example.com")
            repository.save(user)
            val found = repository.findByEmail("test@example.com")

            assertEquals("testuser", found?.username)
            assertEquals("test@example.com", found?.email)
        }

    @Test
    fun `should return null when user not found by email`() =
        runTest {
            val found = repository.findByEmail("nonexistent@example.com")
            assertNull(found)
        }

    @Test
    fun `should find all users`() =
        runTest {
            repository.save(User(0L, "user1", "user1@example.com"))
            repository.save(User(0L, "user2", "user2@example.com"))

            val users = repository.findAll()
            assertEquals(2, users.size)
        }

    @Test
    fun `should check if user exists by email`() =
        runTest {
            repository.save(User(0L, "testuser", "test@example.com"))

            assertTrue(repository.existsByEmail("test@example.com"))
            assertFalse(repository.existsByEmail("nonexistent@example.com"))
        }

    @Test
    fun `should delete user by id`() =
        runTest {
            val user = User(0L, "testuser", "test@example.com")
            val saved = repository.save(user)
            val deleted = repository.deleteById(saved.seq)

            assertTrue(deleted)
            assertNull(repository.findById(saved.seq))
            assertFalse(repository.existsByEmail("test@example.com"))
        }

    @Test
    fun `should return false when deleting non-existent user`() =
        runTest {
            val deleted = repository.deleteById(999L)
            assertFalse(deleted)
        }

    @Test
    fun `should count users correctly`() =
        runTest {
            assertEquals(0, repository.count())

            repository.save(User(0L, "user1", "user1@example.com"))
            assertEquals(1, repository.count())

            repository.save(User(0L, "user2", "user2@example.com"))
            assertEquals(2, repository.count())
        }

    @Test
    fun `should clear all users`() =
        runTest {
            repository.save(User(0L, "user1", "user1@example.com"))
            repository.save(User(0L, "user2", "user2@example.com"))
            assertEquals(2, repository.count())

            repository.clear()
            assertEquals(0, repository.count())
            assertNull(repository.findByEmail("user1@example.com"))
        }

    @Test
    fun `should update user correctly`() =
        runTest {
            val user = User(0L, "testuser", "test@example.com")
            val saved = repository.save(user)
            val updated = saved.copy(username = "newusername")
            repository.save(updated)

            val found = repository.findById(saved.seq)
            assertEquals("newusername", found?.username)
        }

    @Test
    fun `should generate sequential seq numbers`() =
        runTest {
            val user1 = repository.save(User(0L, "user1", "user1@example.com"))
            val user2 = repository.save(User(0L, "user2", "user2@example.com"))

            assertTrue(user1.seq > 0)
            assertTrue(user2.seq > user1.seq)
        }

    @Test
    fun `should handle email index updates when email changes`() =
        runTest {
            val user = User(0L, "testuser", "old@example.com")
            val saved = repository.save(user)
            val updated = saved.copy(email = "new@example.com")
            repository.save(updated)

            assertNull(repository.findByEmail("old@example.com"))
            assertEquals(saved.seq, repository.findByEmail("new@example.com")?.seq)
        }

    @Test
    fun `should reset sequence generator after clear`() =
        runTest {
            val user1 = repository.save(User(0L, "user1", "user1@example.com"))
            assertTrue(user1.seq > 0)

            repository.clear()

            val user2 = repository.save(User(0L, "user2", "user2@example.com"))
            assertEquals(1L, user2.seq)
        }
}
