package com.kangfru.mocktradingsystem.service

import com.kangfru.mocktradingsystem.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = UserRepository()
        userService = UserService(userRepository)
    }

    @Test
    fun `should create user successfully`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")

            assertEquals("testuser", user.username)
            assertEquals("test@example.com", user.email)
            assertTrue(user.seq > 0)
        }

    @Test
    fun `should throw exception when creating user with blank username`() =
        runTest {
            assertThrows<IllegalArgumentException> {
                userService.createUser("", "test@example.com")
            }
        }

    @Test
    fun `should throw exception when creating user with blank email`() =
        runTest {
            assertThrows<IllegalArgumentException> {
                userService.createUser("testuser", "")
            }
        }

    @Test
    fun `should throw exception when creating user with duplicate email`() =
        runTest {
            userService.createUser("user1", "test@example.com")

            assertThrows<IllegalArgumentException> {
                userService.createUser("user2", "test@example.com")
            }
        }

    @Test
    fun `should get user by seq`() =
        runTest {
            val created = userService.createUser("testuser", "test@example.com")
            val retrieved = userService.getUser(created.seq)

            assertEquals(created.seq, retrieved.seq)
            assertEquals(created.username, retrieved.username)
        }

    @Test
    fun `should throw exception when getting non-existent user`() =
        runTest {
            assertThrows<NoSuchElementException> {
                userService.getUser(999L)
            }
        }

    @Test
    fun `should get user by email`() =
        runTest {
            val created = userService.createUser("testuser", "test@example.com")
            val retrieved = userService.getUserByEmail("test@example.com")

            assertEquals(created.seq, retrieved.seq)
            assertEquals(created.email, retrieved.email)
        }

    @Test
    fun `should throw exception when getting user by non-existent email`() =
        runTest {
            assertThrows<NoSuchElementException> {
                userService.getUserByEmail("nonexistent@example.com")
            }
        }

    @Test
    fun `should get all users`() =
        runTest {
            userService.createUser("user1", "user1@example.com")
            userService.createUser("user2", "user2@example.com")

            val users = userService.getAllUsers()
            assertEquals(2, users.size)
        }

    @Test
    fun `should update user username`() =
        runTest {
            val created = userService.createUser("testuser", "test@example.com")
            val updated = userService.updateUser(created.seq, "newusername", null)

            assertEquals("newusername", updated.username)
            assertEquals("test@example.com", updated.email)
        }

    @Test
    fun `should update user email`() =
        runTest {
            val created = userService.createUser("testuser", "test@example.com")
            val updated = userService.updateUser(created.seq, null, "newemail@example.com")

            assertEquals("testuser", updated.username)
            assertEquals("newemail@example.com", updated.email)
        }

    @Test
    fun `should throw exception when updating email to existing one`() =
        runTest {
            userService.createUser("user1", "user1@example.com")
            val user2 = userService.createUser("user2", "user2@example.com")

            assertThrows<IllegalArgumentException> {
                userService.updateUser(user2.seq, null, "user1@example.com")
            }
        }

    @Test
    fun `should delete user successfully`() =
        runTest {
            val created = userService.createUser("testuser", "test@example.com")
            val deleted = userService.deleteUser(created.seq)

            assertTrue(deleted)
            assertThrows<NoSuchElementException> {
                userService.getUser(created.seq)
            }
        }

    @Test
    fun `should return false when deleting non-existent user`() =
        runTest {
            val deleted = userService.deleteUser(999L)
            assertFalse(deleted)
        }

    @Test
    fun `should add account to user`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")
            val updated = userService.addAccountToUser(user.seq, "ACC-001")

            assertEquals(1, updated.accountNumbers.size)
            assertTrue(updated.accountNumbers.contains("ACC-001"))
        }

    @Test
    fun `should throw exception when adding duplicate account`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")
            userService.addAccountToUser(user.seq, "ACC-001")

            assertThrows<IllegalArgumentException> {
                userService.addAccountToUser(user.seq, "ACC-001")
            }
        }

    @Test
    fun `should remove account from user`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")
            userService.addAccountToUser(user.seq, "ACC-001")
            val updated = userService.removeAccountFromUser(user.seq, "ACC-001")

            assertEquals(0, updated.accountNumbers.size)
        }

    @Test
    fun `should throw exception when removing non-existent account`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")

            assertThrows<IllegalArgumentException> {
                userService.removeAccountFromUser(user.seq, "ACC-001")
            }
        }

    @Test
    fun `should get user accounts`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")
            userService.addAccountToUser(user.seq, "ACC-001")
            userService.addAccountToUser(user.seq, "ACC-002")

            val accounts = userService.getUserAccounts(user.seq)
            assertEquals(2, accounts.size)
            assertTrue(accounts.contains("ACC-001"))
            assertTrue(accounts.contains("ACC-002"))
        }

    @Test
    fun `should update updatedAt timestamp when updating user`() =
        runTest {
            val user = userService.createUser("testuser", "test@example.com")
            val originalUpdatedAt = user.updatedAt

            // Small delay to ensure timestamp difference
            Thread.sleep(10)

            val updated = userService.updateUser(user.seq, "newusername", null)

            assertTrue(updated.updatedAt.isAfter(originalUpdatedAt))
        }

    @Nested
    inner class ConcurrencyTests {
        @Test
        fun `should handle concurrent account additions correctly`() =
            runTest {
                // Given
                val user = userService.createUser("testuser", "test@example.com")
                val accountCount = 10

                // When: add 10 accounts concurrently
                val jobs =
                    List(accountCount) { index ->
                        async {
                            userService.addAccountToUser(user.seq, "ACC-${index.toString().padStart(3, '0')}")
                        }
                    }
                jobs.awaitAll()

                // Then: user should have all 10 accounts
                val updatedUser = userService.getUser(user.seq)
                assertEquals(accountCount, updatedUser.accountNumbers.size)
            }

        @Test
        fun `should handle concurrent account removals correctly`() =
            runTest {
                // Given
                val user = userService.createUser("testuser", "test@example.com")
                val accountCount = 10

                // Add 10 accounts first
                repeat(accountCount) { index ->
                    userService.addAccountToUser(user.seq, "ACC-${index.toString().padStart(3, '0')}")
                }

                // When: remove all accounts concurrently
                val jobs =
                    List(accountCount) { index ->
                        async {
                            userService.removeAccountFromUser(user.seq, "ACC-${index.toString().padStart(3, '0')}")
                        }
                    }
                jobs.awaitAll()

                // Then: user should have no accounts
                val updatedUser = userService.getUser(user.seq)
                assertEquals(0, updatedUser.accountNumbers.size)
            }

        @Test
        fun `should handle concurrent updates correctly`() =
            runTest {
                // Given
                val user = userService.createUser("testuser", "test@example.com")
                val updateCount = 10

                // When: update username concurrently (last write wins)
                val jobs =
                    List(updateCount) { index ->
                        async {
                            userService.updateUser(user.seq, "user-$index", null)
                        }
                    }
                jobs.awaitAll()

                // Then: user should have one of the usernames and original email
                val updatedUser = userService.getUser(user.seq)
                assertTrue(updatedUser.username.startsWith("user-"))
                assertEquals("test@example.com", updatedUser.email)
            }

        @Test
        fun `should handle concurrent account additions and removals correctly`() =
            runTest {
                // Given
                val user = userService.createUser("testuser", "test@example.com")
                val operationsPerType = 5

                // Add initial accounts
                repeat(operationsPerType) { index ->
                    userService.addAccountToUser(user.seq, "ACC-INITIAL-${index.toString().padStart(3, '0')}")
                }

                // When: concurrent additions and removals
                val additions =
                    List(operationsPerType) { index ->
                        async {
                            userService.addAccountToUser(user.seq, "ACC-NEW-${index.toString().padStart(3, '0')}")
                        }
                    }
                val removals =
                    List(operationsPerType) { index ->
                        async {
                            userService.removeAccountFromUser(user.seq, "ACC-INITIAL-${index.toString().padStart(3, '0')}")
                        }
                    }
                (additions + removals).awaitAll()

                // Then: user should have exactly 5 accounts (the new ones)
                val updatedUser = userService.getUser(user.seq)
                assertEquals(operationsPerType, updatedUser.accountNumbers.size)
                assertTrue(updatedUser.accountNumbers.all { it.startsWith("ACC-NEW-") })
            }
    }
}
