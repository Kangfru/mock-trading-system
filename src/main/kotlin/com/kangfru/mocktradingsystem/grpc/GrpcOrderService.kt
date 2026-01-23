package com.kangfru.mocktradingsystem.grpc

import com.kangfru.mocktradingsystem.domain.OrderStatus
import com.kangfru.mocktradingsystem.service.ExecutionService
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GrpcOrderService(
    private val executionService: ExecutionService
) : OrderServiceGrpcKt.OrderServiceCoroutineImplBase() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getOrder(request: GetOrderRequest): GetOrderResponse {
        val domainOrder = executionService.getOrder(request.orderNumber)
            ?: throw StatusException(
                Status.NOT_FOUND.withDescription("Order not found: ${request.orderNumber}")
            )

        return getOrderResponse {
            order = domainOrder.toProto()
        }
    }

    override fun watchOrderStatus(request: WatchOrderStatusRequest): Flow<OrderStatusUpdate> = flow {
        while (true) {
            val order = executionService.getOrder(request.orderNumber)
                ?: throw StatusException(
                    Status.NOT_FOUND.withDescription("Order not found: ${request.orderNumber}")
                )

            emit(
                orderStatusUpdate {
                    orderStatus = order.status.toProto()
                }
            )

            if (order.status == OrderStatus.FILLED || order.status == OrderStatus.CANCELLED) {
                break  // 스트림 종료
            }

            delay(500)  // 폴링 간격
        }
    }

    override suspend fun createBulkOrder(requests: Flow<OrderRequest>): OrderResponse {
        var ok = 0
        var ng = 0

        requests.collect { request ->
            try {
                executionService.processOrder(request.toDomainOrder())
                ok++
            } catch (e: Exception) {
                logger.error("error occurued ${e.message}")
                ng++
            }
        }

        return orderResponse {
            successCount = ok
            failCount = ng
        }
    }
}