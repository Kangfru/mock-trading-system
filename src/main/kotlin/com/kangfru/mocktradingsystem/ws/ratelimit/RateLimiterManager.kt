package com.kangfru.mocktradingsystem.ws.ratelimit

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * 세션별 RateLimiter를 관리하는 매니저
 *
 * 각 WebSocket 세션마다 독립적인 RateLimiter를 생성/관리합니다.
 */
@Component
class RateLimiterManager(
    private val maxRequestsPerSecond: Int = 5
) {
    private val limiters = ConcurrentHashMap<String, RateLimiter>()

    /**
     * 해당 세션의 요청을 허용할지 판단합니다.
     * 세션의 RateLimiter가 없으면 새로 생성합니다.
     *
     * @param sessionId WebSocket 세션 ID
     * @return true면 허용, false면 Rate Limit 초과
     */
    fun tryAcquire(sessionId: String): Boolean {
        val limiter = limiters.computeIfAbsent(sessionId) {
            RateLimiter(maxRequestsPerSecond)
        }
        return limiter.tryAcquire()
    }

    /**
     * 해당 세션의 남은 요청 가능 횟수
     */
    fun remainingRequests(sessionId: String): Int {
        return limiters[sessionId]?.remainingRequests() ?: maxRequestsPerSecond
    }

    /**
     * 해당 세션이 다시 요청 가능해지는 시간 (밀리초)
     */
    fun retryAfterMillis(sessionId: String): Long {
        return limiters[sessionId]?.retryAfterMillis() ?: 0
    }

    /**
     * 세션 종료 시 RateLimiter 제거 (자원 정리)
     */
    fun removeSession(sessionId: String) {
        limiters.remove(sessionId)
    }

    /**
     * 현재 관리 중인 세션 수 (모니터링용)
     */
    fun activeSessionCount(): Int = limiters.size
}
