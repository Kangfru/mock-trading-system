package com.kangfru.mocktradingsystem.grpc

import com.kangfru.mocktradingsystem.domain.OrderType
import com.kangfru.mocktradingsystem.domain.Order as DomainOrder
import com.kangfru.mocktradingsystem.service.ExecutionService
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
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
            orderType = OrderType.SELL,
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

}