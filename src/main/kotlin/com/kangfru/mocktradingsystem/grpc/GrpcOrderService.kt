package com.kangfru.mocktradingsystem.grpc

import com.kangfru.mocktradingsystem.domain.OrderStatus
import com.kangfru.mocktradingsystem.service.ExecutionService
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service

@Service
class GrpcOrderService(
    private val executionService: ExecutionService
) : OrderServiceGrpcKt.OrderServiceCoroutineImplBase() {
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
}