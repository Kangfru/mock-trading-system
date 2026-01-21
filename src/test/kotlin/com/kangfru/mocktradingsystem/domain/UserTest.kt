package com.kangfru.mocktradingsystem.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserTest {
    @Test
    fun `should create user successfully`() {
        val user = User(1L, "testuser", "test@example.com")

        assertEquals(1L, user.seq)
        assertEquals("testuser", user.username)
        assertEquals("test@example.com", user.email)
        assertTrue(user.accountNumbers.isEmpty())
    }

    @Test
    fun `should add account successfully`() {
        val user = User(1L, "testuser", "test@example.com")
        val updated = user.addAccount("ACC-001")

        assertEquals(1, updated.accountNumbers.size)
        assertTrue(updated.accountNumbers.contains("ACC-001"))
    }

    @Test
    fun `should throw exception when adding duplicate account`() {
        val user = User(1L, "testuser", "test@example.com", listOf("ACC-001"))

        assertThrows<IllegalArgumentException> {
            user.addAccount("ACC-001")
        }
    }

    @Test
    fun `should throw exception when adding blank account number`() {
        val user = User(1L, "testuser", "test@example.com")

        assertThrows<IllegalArgumentException> {
            user.addAccount("")
        }
    }

    @Test
    fun `should remove account successfully`() {
        val user = User(1L, "testuser", "test@example.com", listOf("ACC-001", "ACC-002"))
        val updated = user.removeAccount("ACC-001")

        assertEquals(1, updated.accountNumbers.size)
        assertTrue(updated.accountNumbers.contains("ACC-002"))
    }

    @Test
    fun `should throw exception when removing non-existent account`() {
        val user = User(1L, "testuser", "test@example.com")

        assertThrows<IllegalArgumentException> {
            user.removeAccount("ACC-001")
        }
    }
}
