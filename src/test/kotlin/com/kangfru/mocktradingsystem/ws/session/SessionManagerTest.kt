package com.kangfru.mocktradingsystem.ws.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.socket.WebSocketSession

class SessionManagerTest {

    private lateinit var sessionManager: SessionManager

    @BeforeEach
    fun setUp() {
        sessionManager = SessionManager()
    }

    @Test
    fun `세션 추가 후 조회 가능`() {
        // Given
        val sessionId = "session-001"
        val mockSession = mock<WebSocketSession>()
        whenever(mockSession.id).thenReturn(sessionId)

        // When
        sessionManager.addSession(mockSession)
        val retrievedSession = sessionManager.getSession(sessionId)

        // Then
        assertNotNull(retrievedSession)
        assertEquals(sessionId, retrievedSession?.id)
    }

    @Test
    fun `세션 제거 후 조회 시 null 반환`() {
        // Given
        val sessionId = "session-002"
        val mockSession = mock<WebSocketSession>()
        whenever(mockSession.id).thenReturn(sessionId)
        sessionManager.addSession(mockSession)

        // When
        sessionManager.removeSession(sessionId)
        val retrievedSession = sessionManager.getSession(sessionId)

        // Then
        assertNull(retrievedSession)
    }

    @Test
    fun `모든 세션 조회`() {
        // Given
        val session1 = mock<WebSocketSession>()
        val session2 = mock<WebSocketSession>()
        val session3 = mock<WebSocketSession>()
        whenever(session1.id).thenReturn("session-001")
        whenever(session2.id).thenReturn("session-002")
        whenever(session3.id).thenReturn("session-003")

        // When
        sessionManager.addSession(session1)
        sessionManager.addSession(session2)
        sessionManager.addSession(session3)
        val allSessions = sessionManager.getAllSessions()

        // Then
        assertEquals(3, allSessions.size)
        assertTrue(allSessions.any { it.id == "session-001" })
        assertTrue(allSessions.any { it.id == "session-002" })
        assertTrue(allSessions.any { it.id == "session-003" })
    }

    @Test
    fun `활성 세션 수 확인`() {
        // Given
        val session1 = mock<WebSocketSession>()
        val session2 = mock<WebSocketSession>()
        whenever(session1.id).thenReturn("session-001")
        whenever(session2.id).thenReturn("session-002")

        // When
        sessionManager.addSession(session1)
        sessionManager.addSession(session2)

        // Then
        assertEquals(2, sessionManager.getActiveSessionCount())
    }

    @Test
    fun `중복 세션 추가 시 기존 세션 유지`() {
        // Given
        val sessionId = "session-001"
        val mockSession1 = mock<WebSocketSession>()
        val mockSession2 = mock<WebSocketSession>()
        whenever(mockSession1.id).thenReturn(sessionId)
        whenever(mockSession2.id).thenReturn(sessionId)

        // When
        sessionManager.addSession(mockSession1)
        sessionManager.addSession(mockSession2)

        // Then
        assertEquals(1, sessionManager.getActiveSessionCount())
        val retrievedSession = sessionManager.getSession(sessionId)
        assertEquals(mockSession1, retrievedSession)
    }

    @Test
    fun `존재하지 않는 세션 조회 시 null 반환`() {
        // Given
        val nonExistentSessionId = "non-existent-session"

        // When
        val retrievedSession = sessionManager.getSession(nonExistentSessionId)

        // Then
        assertNull(retrievedSession)
    }

    @Test
    fun `빈 세션 매니저의 활성 세션 수는 0`() {
        // When
        val count = sessionManager.getActiveSessionCount()

        // Then
        assertEquals(0, count)
    }

    @Test
    fun `빈 세션 매니저의 전체 세션 조회 시 빈 컬렉션 반환`() {
        // When
        val allSessions = sessionManager.getAllSessions()

        // Then
        assertTrue(allSessions.isEmpty())
    }
}
