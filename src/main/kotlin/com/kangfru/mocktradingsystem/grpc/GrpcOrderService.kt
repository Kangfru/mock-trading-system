package com.kangfru.mocktradingsystem.grpc

import com.kangfru.mocktradingsystem.service.ExecutionService
import io.grpc.Status
import io.grpc.StatusException
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
}