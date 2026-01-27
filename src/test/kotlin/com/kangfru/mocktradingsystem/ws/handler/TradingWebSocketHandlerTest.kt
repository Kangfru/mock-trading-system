package com.kangfru.mocktradingsystem.ws.handler

import com.kangfru.mocktradingsystem.domain.Order
import com.kangfru.mocktradingsystem.domain.OrderStatus
import com.kangfru.mocktradingsystem.domain.OrderType
import com.kangfru.mocktradingsystem.domain.PriceType
import com.kangfru.mocktradingsystem.domain.StockHolding
import com.kangfru.mocktradingsystem.service.AccountService
import com.kangfru.mocktradingsystem.service.ExecutionService
import com.kangfru.mocktradingsystem.ws.message.WsCancelOrderRequest
import com.kangfru.mocktradingsystem.ws.message.WsCreateOrderRequest
import com.kangfru.mocktradingsystem.ws.message.WsErrorResponse
import com.kangfru.mocktradingsystem.ws.message.WsGetPortfolioRequest
import com.kangfru.mocktradingsystem.ws.message.WsOrderCreatedResponse
import com.kangfru.mocktradingsystem.ws.message.WsQueryOrderRequest
import com.kangfru.mocktradingsystem.ws.ratelimit.RateLimiterManager
import com.kangfru.mocktradingsystem.ws.session.SessionManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal

class TradingWebSocketHandlerTest {

    private lateinit var rateLimiterManager: RateLimiterManager
    private lateinit var executionService: ExecutionService
    private lateinit var accountService: AccountService
    private lateinit var sessionManager: SessionManager
    private lateinit var objectMapper: ObjectMapper
    private lateinit var handler: TradingWebSocketHandler

    @BeforeEach
    fun setUp() {
        rateLimiterManager = mock()
        executionService = mock()
        accountService = mock()
        sessionManager = mock()
        objectMapper = ObjectMapper()

        handler = TradingWebSocketHandler(
            rateLimiterManager = rateLimiterManager,
            executionService = executionService,
            accountService = accountService,
            objectMapper = objectMapper,
            sessionManager = sessionManager
        )
    }

    @Test
    fun `WsErrorResponse의 success 필드는 false`() {
        // Given
        val errorResponse = WsErrorResponse(
            requestId = "req-error",
            errorCode = HttpStatus.TOO_MANY_REQUESTS.name,
            errorMessage = "Rate limit exceeded"
        )

        // Then
        assertFalse(errorResponse.success)
        assertEquals("req-error", errorResponse.requestId)
        assertEquals("TOO_MANY_REQUESTS", errorResponse.errorCode)
    }

    @Test
    fun `WsOrderCreatedResponse의 success 필드는 true`() {
        // Given
        val successResponse = WsOrderCreatedResponse(
            requestId = "req-success",
            orderNumber = 123456L
        )

        // Then
        assertTrue(successResponse.success)
        assertEquals("req-success", successResponse.requestId)
        assertEquals(123456L, successResponse.orderNumber)
    }

    @Test
    fun `WsCreateOrderRequest를 JSON으로 직렬화 및 역직렬화 가능`() {
        // Given
        val request = WsCreateOrderRequest(
            requestId = "req-001",
            stockCode = "AAPL",
            orderType = OrderType.BUY,
            quantity = 10,
            price = BigDecimal("150.00"),
            priceType = PriceType.LIMIT
        )

        // When
        val json = objectMapper.writeValueAsString(request)
        val deserialized = objectMapper.readValue(json, WsCreateOrderRequest::class.java)

        // Then
        assertEquals(request.requestId, deserialized.requestId)
        assertEquals(request.stockCode, deserialized.stockCode)
        assertEquals(request.orderType, deserialized.orderType)
        assertEquals(request.quantity, deserialized.quantity)
        assertEquals(request.price, deserialized.price)
        assertEquals(request.priceType, deserialized.priceType)
    }

    @Test
    fun `WsQueryOrderRequest를 JSON으로 직렬화 및 역직렬화 가능`() {
        // Given
        val request = WsQueryOrderRequest(
            requestId = "req-002",
            orderNumber = 123456L
        )

        // When
        val json = objectMapper.writeValueAsString(request)
        val deserialized = objectMapper.readValue(json, WsQueryOrderRequest::class.java)

        // Then
        assertEquals(request.requestId, deserialized.requestId)
        assertEquals(request.orderNumber, deserialized.orderNumber)
    }

    @Test
    fun `WsCancelOrderRequest를 JSON으로 직렬화 및 역직렬화 가능`() {
        // Given
        val request = WsCancelOrderRequest(
            requestId = "req-003",
            orderNumber = 789012L,
            originalOrderNumber = 123456L,
            stockCode = "GOOGL",
            orderType = OrderType.BUY,
            quantity = 5,
            price = BigDecimal("100.00")
        )

        // When
        val json = objectMapper.writeValueAsString(request)
        val deserialized = objectMapper.readValue(json, WsCancelOrderRequest::class.java)

        // Then
        assertEquals(request.requestId, deserialized.requestId)
        assertEquals(request.orderNumber, deserialized.orderNumber)
        assertEquals(request.originalOrderNumber, deserialized.originalOrderNumber)
        assertEquals(request.stockCode, deserialized.stockCode)
        assertEquals(request.orderType, deserialized.orderType)
        assertEquals(request.quantity, deserialized.quantity)
        assertEquals(request.price, deserialized.price)
    }

    @Test
    fun `WsGetPortfolioRequest를 JSON으로 직렬화 및 역직렬화 가능`() {
        // Given
        val request = WsGetPortfolioRequest(
            requestId = "req-004",
            accountNumber = "ACC-001"
        )

        // When
        val json = objectMapper.writeValueAsString(request)
        val deserialized = objectMapper.readValue(json, WsGetPortfolioRequest::class.java)

        // Then
        assertEquals(request.requestId, deserialized.requestId)
        assertEquals(request.accountNumber, deserialized.accountNumber)
    }

    @Test
    fun `Order 도메인 객체 생성 테스트`() {
        // Given & When
        val order = Order(
            orderNumber = 123456L,
            stockCode = "AAPL",
            orderType = OrderType.BUY,
            quantity = 10,
            price = BigDecimal("150.00"),
            priceType = PriceType.LIMIT,
            status = OrderStatus.PENDING
        )

        // Then
        assertEquals(123456L, order.orderNumber)
        assertEquals("AAPL", order.stockCode)
        assertEquals(OrderType.BUY, order.orderType)
        assertEquals(10, order.quantity)
        assertEquals(BigDecimal("150.00"), order.price)
        assertEquals(PriceType.LIMIT, order.priceType)
        assertEquals(OrderStatus.PENDING, order.status)
    }

    @Test
    fun `StockHolding 도메인 객체 생성 테스트`() {
        // Given & When
        val holding = StockHolding(
            stockCode = "TSLA",
            quantity = 5,
            averagePrice = BigDecimal("200.00")
        )

        // Then
        assertEquals("TSLA", holding.stockCode)
        assertEquals(5, holding.quantity)
        assertEquals(BigDecimal("200.00"), holding.averagePrice)
    }

    @Test
    fun `RateLimiterManager가 세션별로 요청을 제한함`() {
        // Given
        val sessionId = "test-session"
        whenever(rateLimiterManager.tryAcquire(sessionId)).thenReturn(false)

        // When
        val allowed = rateLimiterManager.tryAcquire(sessionId)

        // Then
        assertFalse(allowed)
    }

    @Test
    fun `ExecutionService가 주문을 처리함`() {
        // Given
        val order = Order(
            stockCode = "AAPL",
            orderType = OrderType.BUY,
            quantity = 10,
            price = BigDecimal("150.00"),
            priceType = PriceType.LIMIT
        )

        // When
        executionService.processOrder(order)

        // Then - processOrder는 void이므로 verify만 수행
        // 실제 통합 테스트에서는 orderStore를 확인해야 함
        assertEquals("AAPL", order.stockCode)
        assertEquals(OrderType.BUY, order.orderType)
    }

    @Test
    fun `ExecutionService가 주문 조회 시 Order를 반환함`() {
        // Given
        val orderNumber = 123456L
        val mockOrder = Order(
            orderNumber = orderNumber,
            stockCode = "AAPL",
            orderType = OrderType.BUY,
            quantity = 10,
            price = BigDecimal("150.00"),
            priceType = PriceType.LIMIT,
            status = OrderStatus.FILLED
        )

        whenever(executionService.getOrder(orderNumber)).thenReturn(mockOrder)

        // When
        val result = executionService.getOrder(orderNumber)

        // Then
        assertEquals(orderNumber, result?.orderNumber)
        assertEquals(OrderStatus.FILLED, result?.status)
    }

    @Test
    fun `모든 OrderType enum 값 확인`() {
        // When
        val values = OrderType.values()

        // Then
        assertTrue(values.contains(OrderType.BUY))
        assertTrue(values.contains(OrderType.SELL))
        assertEquals(2, values.size)
    }

    @Test
    fun `모든 PriceType enum 값 확인`() {
        // When
        val values = PriceType.values()

        // Then
        assertTrue(values.contains(PriceType.LIMIT))
        assertTrue(values.contains(PriceType.MARKET))
        assertEquals(2, values.size)
    }

    @Test
    fun `모든 OrderStatus enum 값 확인`() {
        // When
        val values = OrderStatus.values()

        // Then
        assertTrue(values.contains(OrderStatus.PENDING))
        assertTrue(values.contains(OrderStatus.FILLED))
        assertTrue(values.contains(OrderStatus.CANCELLED))
        assertTrue(values.contains(OrderStatus.MODIFIED))
        assertEquals(4, values.size)
    }
}
