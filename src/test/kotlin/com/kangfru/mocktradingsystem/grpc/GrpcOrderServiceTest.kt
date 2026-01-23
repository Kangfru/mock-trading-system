package com.kangfru.mocktradingsystem.grpc

import com.kangfru.mocktradingsystem.domain.Order as DomainOrder
import com.kangfru.mocktradingsystem.domain.OrderType as DomainOrderType
import com.kangfru.mocktradingsystem.service.ExecutionService
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.test.Test

class GrpcOrderServiceTest {
    private lateinit var executionService: ExecutionService  // Mock
    private lateinit var grpcOrderService: GrpcOrderService

    @BeforeEach
    fun setUp() {
        executionService = Mockito.mock(ExecutionService::class.java)
        grpcOrderService = GrpcOrderService(executionService)
    }

    @Test
    fun `should return order when order exists`() = runTest {
        // Given
        val testOrder = DomainOrder(
            orderNumber = 123L,
            stockCode = "001230",
            orderType = DomainOrderType.SELL,
            quantity = 1,
            price = BigDecimal(1000)
        )
        Mockito.`when`(executionService.getOrder(123L)).thenReturn(testOrder)

        // When
        val request = getOrderRequest { orderNumber = 123L }
        val response = grpcOrderService.getOrder(request)

        // Then
        assertEquals(123L, response.order.orderNumber)
    }

    @Test
    fun `should throw NOT_FOUND when order does not exist`() = runTest {
        // Given
        Mockito.`when`(executionService.getOrder(999L)).thenReturn(null)

        // When & Then
        val request = getOrderRequest { orderNumber = 999L }
        val exception = assertThrows<StatusException> {
            grpcOrderService.getOrder(request)
        }

        assertEquals(Status.NOT_FOUND.code, exception.status.code)
    }

    @Test
    fun `createBulkOrder should return success count when all orders succeed`() = runTest {
        // Given
        doNothing().whenever(executionService).processOrder(any())

        val requests = flowOf(
            orderRequest {
                stockCode = "005930"
                orderType = OrderType.ORDER_TYPE_BUY
                quantity = 10
                price = "50000"
                priceType = PriceType.PRICE_TYPE_LIMIT
            },
            orderRequest {
                stockCode = "000660"
                orderType = OrderType.ORDER_TYPE_SELL
                quantity = 5
                price = "120000"
                priceType = PriceType.PRICE_TYPE_LIMIT
            }
        )

        // When
        val response = grpcOrderService.createBulkOrder(requests)

        // Then
        assertEquals(2, response.successCount)
        assertEquals(0, response.failCount)
    }

    @Test
    fun `createBulkOrder should return fail count when some orders fail`() = runTest {
        // Given
        doNothing()
            .doThrow(RuntimeException("Order failed"))
            .doNothing()
            .whenever(executionService).processOrder(any())

        val requests = flowOf(
            orderRequest {
                stockCode = "005930"
                orderType = OrderType.ORDER_TYPE_BUY
                quantity = 10
                price = "50000"
                priceType = PriceType.PRICE_TYPE_LIMIT
            },
            orderRequest {
                stockCode = "000660"
                orderType = OrderType.ORDER_TYPE_BUY
                quantity = 5
                price = "120000"
                priceType = PriceType.PRICE_TYPE_LIMIT
            },
            orderRequest {
                stockCode = "035720"
                orderType = OrderType.ORDER_TYPE_SELL
                quantity = 20
                price = "80000"
                priceType = PriceType.PRICE_TYPE_LIMIT
            }
        )

        // When
        val response = grpcOrderService.createBulkOrder(requests)

        // Then
        assertEquals(2, response.successCount)
        assertEquals(1, response.failCount)
    }

    @Test
    fun `createBulkOrder should return zero counts when stream is empty`() = runTest {
        // Given
        val requests = emptyFlow<OrderRequest>()

        // When
        val response = grpcOrderService.createBulkOrder(requests)

        // Then
        assertEquals(0, response.successCount)
        assertEquals(0, response.failCount)
    }

}