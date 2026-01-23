package com.kangfru.mocktradingsystem.ws.ratelimit

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Fixed Window 기반 Rate Limiter
 *
 * 매 1초마다 카운터가 리셋되고, 초당 maxRequests 회까지 요청을 허용합니다.
 *
 * @param maxRequests 초당 최대 허용 요청 수 (기본값: 5)
 */
class RateLimiter(
    private val maxRequests: Int = 5
) {
    // 현재 윈도우(1초)의 시작 시간 (초 단위)
    private val windowStart = AtomicLong(currentSecond())

    // 현재 윈도우에서의 요청 횟수
    private val requestCount = AtomicInteger(0)

    /**
     * 요청을 허용할지 판단합니다.
     *
     * @return true면 허용, false면 Rate Limit 초과
     */
    fun tryAcquire(): Boolean {
        val now = currentSecond()
        val currentWindow = windowStart.get()

        // 새로운 윈도우(초)로 넘어갔으면 리셋
        if (now > currentWindow) {
            // CAS로 윈도우 업데이트 시도
            if (windowStart.compareAndSet(currentWindow, now)) {
                requestCount.set(1)
                return true
            }
            // 다른 스레드가 이미 업데이트했으면 재시도
            return tryAcquire()
        }

        // 현재 윈도우 내에서 카운트 증가
        val count = requestCount.incrementAndGet()
        return count <= maxRequests
    }

    /**
     * 현재 남은 요청 가능 횟수
     */
    fun remainingRequests(): Int {
        val now = currentSecond()
        if (now > windowStart.get()) {
            return maxRequests  // 새 윈도우면 전체 허용
        }
        return (maxRequests - requestCount.get()).coerceAtLeast(0)
    }

    /**
     * 다음 윈도우까지 남은 시간 (밀리초)
     */
    fun retryAfterMillis(): Long {
        val now = System.currentTimeMillis()
        val nextWindow = (windowStart.get() + 1) * 1000
        return (nextWindow - now).coerceAtLeast(0)
    }

    private fun currentSecond(): Long = System.currentTimeMillis() / 1000
}
