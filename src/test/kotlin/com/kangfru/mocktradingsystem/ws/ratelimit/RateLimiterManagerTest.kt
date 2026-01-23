package com.kangfru.mocktradingsystem.ws.ratelimit

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RateLimiterManagerTest {

    private lateinit var manager: RateLimiterManager

    @BeforeEach
    fun setUp() {
        manager = RateLimiterManager(maxRequestsPerSecond = 5)
    }

    @Test
    fun `should create limiter for new session`() {
        // Given
        val sessionId = "session-1"

        // When
        val allowed = manager.tryAcquire(sessionId)

        // Then
        assertTrue(allowed)
        assertEquals(1, manager.activeSessionCount())
    }

    @Test
    fun `should isolate rate limits per session`() {
        // Given
        val session1 = "session-1"
        val session2 = "session-2"

        // When: session1 토큰 모두 소비
        repeat(5) { manager.tryAcquire(session1) }

        // Then: session1은 거부, session2는 허용
        assertFalse(manager.tryAcquire(session1), "session1은 거부되어야 함")
        assertTrue(manager.tryAcquire(session2), "session2는 허용되어야 함")
    }

    @Test
    fun `should remove session on cleanup`() {
        // Given
        val sessionId = "session-1"
        manager.tryAcquire(sessionId)
        assertEquals(1, manager.activeSessionCount())

        // When
        manager.removeSession(sessionId)

        // Then
        assertEquals(0, manager.activeSessionCount())
    }

    @Test
    fun `should return remaining requests for session`() {
        // Given
        val sessionId = "session-1"
        repeat(3) { manager.tryAcquire(sessionId) }

        // When
        val remaining = manager.remainingRequests(sessionId)

        // Then
        assertEquals(2, remaining)
    }

    @Test
    fun `should return max requests for unknown session`() {
        // Given
        val unknownSession = "unknown"

        // When
        val remaining = manager.remainingRequests(unknownSession)

        // Then
        assertEquals(5, remaining)
    }
}
