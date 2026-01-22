package com.kangfru.mocktradingsystem.service

import com.kangfru.mocktradingsystem.domain.*
import com.kangfru.mocktradingsystem.util.SnowflakeIdGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class ExecutionService(
    private val snowflakeIdGenerator: SnowflakeIdGenerator,
    private val orderBookService: OrderBookService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // 주문 저장소 (orderNumber -> Order)
    private val orderStore = ConcurrentHashMap<Long, Order>()

    fun processOrder(order: Order) {
        when (order.action) {
            OrderAction.NEW -> executeNewOrder(order)
            OrderAction.CANCEL -> executeCancelOrder(order)
            OrderAction.MODIFY -> executeModifyOrder(order)
        }
    }

    private fun executeNewOrder(order: Order) {
        // 주문 저장
        orderStore[order.orderNumber] = order

        // OrderBook을 통한 매칭
        val result =
            when (order.orderType) {
                OrderType.BUY -> orderBookService.processBuyOrder(order)
                OrderType.SELL -> orderBookService.processSellOrder(order)
            }

        // 주문 상태 업데이트
        val newStatus =
            if (result.remainingQuantity == 0) {
                OrderStatus.FILLED
            } else if (result.executions.isEmpty()) {
                OrderStatus.PENDING
            } else {
                OrderStatus.PENDING // 부분 체결
            }
        orderStore[order.orderNumber] = order.copy(status = newStatus)

        // 매칭된 상대방 주문 상태 업데이트
        for (matchedOrder in result.matchedOrders) {
            val counterOrder = orderStore[matchedOrder.orderNumber]
            if (counterOrder != null) {
                val counterStatus = if (matchedOrder.remainingQuantity == 0) {
                    OrderStatus.FILLED
                } else {
                    OrderStatus.PENDING // 부분 체결
                }
                orderStore[matchedOrder.orderNumber] = counterOrder.copy(status = counterStatus)
                logger.info(
                    "[COUNTER] 상대 주문 상태 업데이트: orderNumber={}, status={}",
                    matchedOrder.orderNumber,
                    counterStatus,
                )
            }
        }

        if (result.executions.isNotEmpty()) {
            logger.info(
                "[NEW] {}건 체결, 잔량 {}주 | orderNumber: {}",
                result.executions.size,
                result.remainingQuantity,
                order.orderNumber,
            )
        }
    }

    private fun executeCancelOrder(order: Order) {
        val originalOrderNumber =
            order.originalOrderNumber
                ?: run {
                    logger.warn("[CANCEL] 원주문 번호 없음: {}", order.orderNumber)
                    return
                }

        val originalOrder = orderStore[originalOrderNumber]
        if (originalOrder == null) {
            logger.warn("[CANCEL] 원주문 없음: originalOrderNumber={}", originalOrderNumber)
            return
        }

        if (originalOrder.status == OrderStatus.CANCELLED) {
            logger.warn("[CANCEL] 이미 취소된 주문: originalOrderNumber={}", originalOrderNumber)
            return
        }

        // 호가창에서 제거
        orderBookService.cancelOrder(originalOrder.stockCode, originalOrderNumber, originalOrder.orderType)

        // 주문 상태 업데이트
        orderStore[originalOrderNumber] = originalOrder.copy(status = OrderStatus.CANCELLED)

        logger.info(
            "[CANCEL] 취소 완료: originalOrderNumber={} | cancelOrderNumber={}",
            originalOrderNumber,
            order.orderNumber,
        )
    }

    private fun executeModifyOrder(order: Order) {
        val originalOrderNumber =
            order.originalOrderNumber
                ?: run {
                    logger.warn("[MODIFY] 원주문 번호 없음: {}", order.orderNumber)
                    return
                }

        val originalOrder = orderStore[originalOrderNumber]
        if (originalOrder == null) {
            logger.warn("[MODIFY] 원주문 없음: originalOrderNumber={}", originalOrderNumber)
            return
        }

        if (originalOrder.status == OrderStatus.CANCELLED) {
            logger.warn("[MODIFY] 취소된 주문은 정정 불가: originalOrderNumber={}", originalOrderNumber)
            return
        }

        // 호가창에서 원주문 제거
        orderBookService.cancelOrder(originalOrder.stockCode, originalOrderNumber, originalOrder.orderType)

        // 원주문 상태를 MODIFIED로 변경
        orderStore[originalOrderNumber] = originalOrder.copy(status = OrderStatus.MODIFIED)

        // 정정된 값 적용
        val newQuantity = if (order.quantity > 0) order.quantity else originalOrder.quantity
        val newPrice = if (order.price > java.math.BigDecimal.ZERO) order.price else originalOrder.price

        // 새로운 주문으로 매칭 시도
        val modifiedOrder =
            originalOrder.copy(
                orderNumber = order.orderNumber,
                quantity = newQuantity,
                price = newPrice,
                action = OrderAction.NEW,
            )
        orderStore[order.orderNumber] = modifiedOrder

        val result =
            when (modifiedOrder.orderType) {
                OrderType.BUY -> orderBookService.processBuyOrder(modifiedOrder)
                OrderType.SELL -> orderBookService.processSellOrder(modifiedOrder)
            }

        // 주문 상태 업데이트
        val newStatus = if (result.remainingQuantity == 0) OrderStatus.FILLED else OrderStatus.PENDING
        orderStore[order.orderNumber] = modifiedOrder.copy(status = newStatus)

        // 매칭된 상대방 주문 상태 업데이트
        for (matchedOrder in result.matchedOrders) {
            val counterOrder = orderStore[matchedOrder.orderNumber]
            if (counterOrder != null) {
                val counterStatus = if (matchedOrder.remainingQuantity == 0) {
                    OrderStatus.FILLED
                } else {
                    OrderStatus.PENDING
                }
                orderStore[matchedOrder.orderNumber] = counterOrder.copy(status = counterStatus)
                logger.info(
                    "[COUNTER] 상대 주문 상태 업데이트: orderNumber={}, status={}",
                    matchedOrder.orderNumber,
                    counterStatus,
                )
            }
        }

        logger.info(
            "[MODIFY] 정정 완료: originalOrderNumber={} → modifyOrderNumber={}, {}건 체결, 잔량 {}주",
            originalOrderNumber,
            order.orderNumber,
            result.executions.size,
            result.remainingQuantity,
        )
    }

    fun getOrder(orderNumber: Long): Order? = orderStore[orderNumber]

    fun getAllOrders(): List<Order> = orderStore.values.toList()

    fun getAllExecutions(): List<Execution> = orderBookService.getExecutions()

    fun getOrderCount(): Int = orderStore.size

    fun getExecutionCount(): Int = orderBookService.getExecutionCount()

    fun getStats(): Map<String, Any> {
        val orders = orderStore.values
        return mapOf(
            "totalOrders" to orders.size,
            "pendingOrders" to orders.count { it.status == OrderStatus.PENDING },
            "filledOrders" to orders.count { it.status == OrderStatus.FILLED },
            "cancelledOrders" to orders.count { it.status == OrderStatus.CANCELLED },
            "modifiedOrders" to orders.count { it.status == OrderStatus.MODIFIED },
            "totalExecutions" to orderBookService.getExecutionCount(),
        )
    }
}
