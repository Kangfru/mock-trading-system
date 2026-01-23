package com.kangfru.mocktradingsystem.ws.ratelimit

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RateLimiterTest {

    @Test
    fun `should allow requests up to max limit`() {
        // Given
        val limiter = RateLimiter(maxRequests = 5)

        // When & Then
        repeat(5) { i ->
            assertTrue(limiter.tryAcquire(), "요청 ${i + 1}은 허용되어야 함")
        }
    }

    @Test
    fun `should reject requests exceeding max limit`() {
        // Given
        val limiter = RateLimiter(maxRequests = 5)

        // When: 5번 허용
        repeat(5) {
            limiter.tryAcquire()
        }

        // Then: 6번째는 거부
        assertFalse(limiter.tryAcquire(), "6번째 요청은 거부되어야 함")
        assertFalse(limiter.tryAcquire(), "7번째 요청도 거부되어야 함")
    }

    @Test
    fun `should return correct remaining requests`() {
        // Given
        val limiter = RateLimiter(maxRequests = 5)

        // When & Then
        assertEquals(5, limiter.remainingRequests())

        limiter.tryAcquire()
        assertEquals(4, limiter.remainingRequests())

        limiter.tryAcquire()
        limiter.tryAcquire()
        assertEquals(2, limiter.remainingRequests())
    }

    @Test
    fun `should reset after window passes`() {
        // Given
        val limiter = RateLimiter(maxRequests = 5)

        // When: 모든 토큰 소비
        repeat(5) { limiter.tryAcquire() }
        assertFalse(limiter.tryAcquire(), "토큰 소진 후 거부")

        // Then: 1초 후 리셋 (실제 테스트에서는 시간이 걸림)
        Thread.sleep(1100)  // 1.1초 대기
        assertTrue(limiter.tryAcquire(), "새 윈도우에서는 허용")
    }

    @Test
    fun `should return retry after millis`() {
        // Given
        val limiter = RateLimiter(maxRequests = 5)
        limiter.tryAcquire()  // 윈도우 시작

        // When
        val retryAfter = limiter.retryAfterMillis()

        // Then: 0~1000ms 사이여야 함
        assertTrue(retryAfter in 0..1000, "retryAfter는 0~1000ms 사이: $retryAfter")
    }
}
