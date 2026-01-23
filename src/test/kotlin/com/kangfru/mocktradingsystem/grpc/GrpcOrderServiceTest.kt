package com.kangfru.mocktradingsystem.grpc

import com.kangfru.mocktradingsystem.domain.Order as DomainOrder
import com.kangfru.mocktradingsystem.domain.OrderType as DomainOrderType
import com.kangfru.mocktradingsystem.service.ExecutionService
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
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

    @Test
    fun `manageOrders should return success response for new order`() = runTest {
        // Given
        doNothing().whenever(executionService).processOrder(any())

        val requests = flowOf(
            orderManagementRequest {
                order = order {
                    orderNumber = 1L
                    stockCode = "005930"
                    orderType = OrderType.ORDER_TYPE_BUY
                    quantity = 10
                    price = "50000"
                    priceType = PriceType.PRICE_TYPE_LIMIT
                    action = OrderAction.ORDER_ACTION_NEW
                }
                clientRequestId = 100L
            }
        )

        // When
        val responses = grpcOrderService.manageOrders(requests).toList()

        // Then
        assertEquals(1, responses.size)
        assertEquals(100L, responses[0].clientRequestId)
        assertEquals(true, responses[0].success)
        assertEquals("", responses[0].errorMessage)
    }

    @Test
    fun `manageOrders should return failure response when order processing fails`() = runTest {
        // Given
        whenever(executionService.processOrder(any())).thenThrow(RuntimeException("Insufficient balance"))

        val requests = flowOf(
            orderManagementRequest {
                order = order {
                    orderNumber = 2L
                    stockCode = "005930"
                    orderType = OrderType.ORDER_TYPE_BUY
                    quantity = 10
                    price = "50000"
                    priceType = PriceType.PRICE_TYPE_LIMIT
                    action = OrderAction.ORDER_ACTION_NEW
                }
                clientRequestId = 200L
            }
        )

        // When
        val responses = grpcOrderService.manageOrders(requests).toList()

        // Then
        assertEquals(1, responses.size)
        assertEquals(200L, responses[0].clientRequestId)
        assertEquals(false, responses[0].success)
        assertEquals("Insufficient balance", responses[0].errorMessage)
    }

    @Test
    fun `manageOrders should handle mixed success and failure`() = runTest {
        // Given
        doNothing()
            .doThrow(RuntimeException("Order failed"))
            .doNothing()
            .whenever(executionService).processOrder(any())

        val requests = flowOf(
            orderManagementRequest {
                order = order {
                    orderNumber = 1L
                    stockCode = "005930"
                    orderType = OrderType.ORDER_TYPE_BUY
                    quantity = 10
                    price = "50000"
                    priceType = PriceType.PRICE_TYPE_LIMIT
                    action = OrderAction.ORDER_ACTION_NEW
                }
                clientRequestId = 1L
            },
            orderManagementRequest {
                order = order {
                    orderNumber = 2L
                    stockCode = "000660"
                    orderType = OrderType.ORDER_TYPE_BUY
                    quantity = 5
                    price = "120000"
                    priceType = PriceType.PRICE_TYPE_LIMIT
                    action = OrderAction.ORDER_ACTION_NEW
                }
                clientRequestId = 2L
            },
            orderManagementRequest {
                order = order {
                    orderNumber = 3L
                    stockCode = "035720"
                    orderType = OrderType.ORDER_TYPE_SELL
                    quantity = 20
                    price = "80000"
                    priceType = PriceType.PRICE_TYPE_LIMIT
                    action = OrderAction.ORDER_ACTION_NEW
                }
                clientRequestId = 3L
            }
        )

        // When
        val responses = grpcOrderService.manageOrders(requests).toList()

        // Then
        assertEquals(3, responses.size)
        assertEquals(true, responses[0].success)
        assertEquals(false, responses[1].success)
        assertEquals("Order failed", responses[1].errorMessage)
        assertEquals(true, responses[2].success)
    }

    @Test
    fun `manageOrders should return empty list when stream is empty`() = runTest {
        // Given
        val requests = emptyFlow<OrderManagementRequest>()

        // When
        val responses = grpcOrderService.manageOrders(requests).toList()

        // Then
        assertEquals(0, responses.size)
    }

    @Test
    fun `manageOrders should handle cancel order action`() = runTest {
        // Given
        doNothing().whenever(executionService).processOrder(any())

        val requests = flowOf(
            orderManagementRequest {
                order = order {
                    orderNumber = 10L
                    stockCode = "005930"
                    orderType = OrderType.ORDER_TYPE_BUY
                    quantity = 10
                    price = "50000"
                    priceType = PriceType.PRICE_TYPE_LIMIT
                    action = OrderAction.ORDER_ACTION_CANCEL
                    originalOrderNumber = 1L
                }
                clientRequestId = 300L
            }
        )

        // When
        val responses = grpcOrderService.manageOrders(requests).toList()

        // Then
        assertEquals(1, responses.size)
        assertEquals(300L, responses[0].clientRequestId)
        assertEquals(true, responses[0].success)
    }

}