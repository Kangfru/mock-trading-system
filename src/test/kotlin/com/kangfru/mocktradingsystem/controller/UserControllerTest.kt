package com.kangfru.mocktradingsystem.controller

import com.kangfru.mocktradingsystem.repository.AccountRepository
import com.kangfru.mocktradingsystem.repository.UserRepository
import com.kangfru.mocktradingsystem.service.AccountService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
class UserControllerTest {
    @Autowired
    private lateinit var userController: UserController

    @Autowired
    private lateinit var accountService: AccountService

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
    fun `should create user successfully`() =
        runTest {
            val request = CreateUserRequest("testuser", "test@example.com")
            val response = userController.createUser(request)

            assertEquals("testuser", response.username)
            assertEquals("test@example.com", response.email)
            assertTrue(response.seq > 0)
        }

    @Test
    fun `should get user by seq`() =
        runTest {
            val request = CreateUserRequest("testuser", "test@example.com")
            val created = userController.createUser(request)
            val retrieved = userController.getUser(created.seq)

            assertEquals(created.seq, retrieved.seq)
            assertEquals("testuser", retrieved.username)
        }

    @Test
    fun `should get user by email`() =
        runTest {
            val request = CreateUserRequest("testuser", "test@example.com")
            userController.createUser(request)
            val retrieved = userController.getUserByEmail("test@example.com")

            assertEquals("test@example.com", retrieved.email)
            assertEquals("testuser", retrieved.username)
        }

    @Test
    fun `should get all users`() =
        runTest {
            userController.createUser(CreateUserRequest("user1", "user1@example.com"))
            userController.createUser(CreateUserRequest("user2", "user2@example.com"))

            val users = userController.getAllUsers()
            assertEquals(2, users.size)
        }

    @Test
    fun `should update user successfully`() =
        runTest {
            val created = userController.createUser(CreateUserRequest("testuser", "test@example.com"))
            val updated = userController.updateUser(created.seq, UpdateUserRequest("newusername", null))

            assertEquals("newusername", updated.username)
            assertEquals("test@example.com", updated.email)
        }

    @Test
    fun `should delete user successfully`() =
        runTest {
            val created = userController.createUser(CreateUserRequest("testuser", "test@example.com"))
            userController.deleteUser(created.seq)

            // User should be deleted (exception expected when trying to get)
            val exception = kotlin.runCatching { userController.getUser(created.seq) }.exceptionOrNull()
            assertTrue(exception is NoSuchElementException)
        }

    @Test
    fun `should get user accounts`() =
        runTest {
            val user = userController.createUser(CreateUserRequest("testuser", "test@example.com"))
            accountService.createAccount(user.seq, BigDecimal("1000.00"))

            val accounts = userController.getUserAccounts(user.seq)
            assertEquals(1, accounts.size)
            assertEquals(BigDecimal("1000.00"), accounts[0].balance)
        }
}
