package com.kangfru.mocktradingsystem.ws.session

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Component
class SessionManager {

    private val logger = LoggerFactory.getLogger(javaClass)

    // session 저장소
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    fun addSession(session: WebSocketSession) {
        sessions.putIfAbsent(session.id, session)
        logger.info("Session ${session.id} connected : active sessions - ${sessions.size}")
    }

    fun removeSession(sessionId: String) {
        sessions.remove(sessionId)
        logger.info("Session $sessionId deleted : active sessions - ${sessions.size}")
    }

    fun getSession(sessionId: String): WebSocketSession? {
        return sessions[sessionId]
    }

    fun getAllSessions(): Collection<WebSocketSession> {
        return sessions.values.toList()
    }

    fun getActiveSessionCount(): Int = sessions.size

}