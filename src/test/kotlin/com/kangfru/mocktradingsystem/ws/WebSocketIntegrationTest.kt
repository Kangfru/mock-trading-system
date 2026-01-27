package com.kangfru.mocktradingsystem.ws

import com.kangfru.mocktradingsystem.ws.handler.TradingWebSocketHandler
import com.kangfru.mocktradingsystem.ws.ratelimit.RateLimiterManager
import com.kangfru.mocktradingsystem.ws.session.SessionManager
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext

/**
 * WebSocket 통합 테스트
 *
 * Spring Context 로드 및 빈 등록을 확인하는 테스트입니다.
 * 실제 WebSocket 연결 테스트는 Kafka 등 인프라 의존성이 필요하여
 * 별도의 E2E 테스트 환경에서 수행하는 것을 권장합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {

    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun `Spring Context가 정상적으로 로드됨`() {
        // Given & When
        val beanNames = context.beanDefinitionNames.toList()

        // Then
        assertTrue(beanNames.contains("tradingWebSocketHandler"))
        assertTrue(beanNames.contains("sessionManager"))
        assertTrue(beanNames.contains("rateLimiterManager"))
    }

    @Test
    fun `TradingWebSocketHandler 빈이 정상 등록됨`() {
        // When
        val handler = context.getBean("tradingWebSocketHandler", TradingWebSocketHandler::class.java)

        // Then
        assertNotNull(handler)
    }

    @Test
    fun `SessionManager 빈이 정상 등록됨`() {
        // When
        val sessionManager = context.getBean("sessionManager", SessionManager::class.java)

        // Then
        assertNotNull(sessionManager)
        assertTrue(sessionManager.getActiveSessionCount() >= 0)
    }

    @Test
    fun `RateLimiterManager 빈이 정상 등록됨`() {
        // When
        val rateLimiterManager = context.getBean("rateLimiterManager", RateLimiterManager::class.java)

        // Then
        assertNotNull(rateLimiterManager)
        assertTrue(rateLimiterManager.activeSessionCount() >= 0)
    }

    @Test
    fun `WebSocket 설정이 정상적으로 등록됨`() {
        // When
        val handlerMapping = context.getBean("handlerMapping")

        // Then
        assertNotNull(handlerMapping)
    }
}
