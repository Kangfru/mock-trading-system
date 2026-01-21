package com.kangfru.mocktradingsystem.service

import com.kangfru.mocktradingsystem.domain.BookOrder
import com.kangfru.mocktradingsystem.domain.Execution
import com.kangfru.mocktradingsystem.domain.Order
import com.kangfru.mocktradingsystem.domain.OrderBook
import com.kangfru.mocktradingsystem.domain.OrderBookSnapshot
import com.kangfru.mocktradingsystem.domain.OrderType
import com.kangfru.mocktradingsystem.util.SnowflakeIdGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * 체결 결과
 */
data class MatchResult(
    val executions: List<Execution>,
    val remainingQuantity: Int,
)

@Service
class OrderBookService(
    private val snowflakeIdGenerator: SnowflakeIdGenerator,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // 종목별 호가창
    private val orderBooks = ConcurrentHashMap<String, OrderBook>()

    // 체결 저장소
    private val executionStore = ConcurrentHashMap<Long, Execution>()

    /**
     * 호가창 가져오기 (없으면 생성)
     */
    fun getOrderBook(stockCode: String): OrderBook = orderBooks.computeIfAbsent(stockCode) { OrderBook(it) }

    /**
     * 호가창 스냅샷 조회
     */
    fun getOrderBookSnapshot(stockCode: String): OrderBookSnapshot = getOrderBook(stockCode).getSnapshot()

    /**
     * 모든 종목의 호가창 조회
     */
    fun getAllOrderBookSnapshots(): Map<String, OrderBookSnapshot> = orderBooks.mapValues { it.value.getSnapshot() }

    /**
     * 매수 주문 처리 (연속 체결)
     * - 매도 호가와 매칭 시도
     * - 미체결 수량은 호가창에 추가
     */
    fun processBuyOrder(order: Order): MatchResult {
        val orderBook = getOrderBook(order.stockCode)
        val executions = mutableListOf<Execution>()
        var remainingQty = order.quantity

        // 매도 호가와 매칭 시도 (가격 조건: 매수가 >= 매도가)
        while (remainingQty > 0) {
            val bestAskPrice = orderBook.getBestAskPrice() ?: break

            // 매수 지정가보다 매도호가가 높으면 매칭 불가
            if (order.price < bestAskPrice) break

            val askOrder = orderBook.pollFirstAskOrderAt(bestAskPrice) ?: break

            // 체결 수량 결정
            val matchQty = minOf(remainingQty, askOrder.remainingQuantity)
            val matchPrice = askOrder.price // 먼저 들어온 주문 가격으로 체결

            // 체결 생성
            val execution =
                Execution(
                    executionId = snowflakeIdGenerator.nextId(),
                    orderNumber = order.orderNumber,
                    orderId = order.orderId,
                    stockCode = order.stockCode,
                    orderType = OrderType.BUY,
                    executedQuantity = matchQty,
                    executedPrice = matchPrice,
                )
            executions.add(execution)
            executionStore[execution.executionId] = execution

            logger.info(
                "[MATCH] 매수 체결: {} {}주 @ {}원 | buyOrder={}, askOrder={}",
                order.stockCode,
                matchQty,
                matchPrice,
                order.orderNumber,
                askOrder.orderNumber,
            )

            remainingQty -= matchQty

            // 매도 주문에 잔량이 있으면 다시 호가창에 추가
            val askRemaining = askOrder.remainingQuantity - matchQty
            if (askRemaining > 0) {
                orderBook.addAskOrder(askOrder.copy(remainingQuantity = askRemaining))
            }
        }

        // 미체결 수량이 있으면 호가창에 추가
        if (remainingQty > 0) {
            val bookOrder =
                BookOrder(
                    orderNumber = order.orderNumber,
                    orderId = order.orderId,
                    price = order.price,
                    remainingQuantity = remainingQty,
                    originalQuantity = order.quantity,
                    orderTime = order.orderTime,
                )
            orderBook.addBidOrder(bookOrder)
            logger.info(
                "[BOOK] 매수 호가 추가: {} {}주 @ {}원 | orderNumber={}",
                order.stockCode,
                remainingQty,
                order.price,
                order.orderNumber,
            )
        }

        return MatchResult(executions, remainingQty)
    }

    /**
     * 매도 주문 처리 (연속 체결)
     * - 매수 호가와 매칭 시도
     * - 미체결 수량은 호가창에 추가
     */
    fun processSellOrder(order: Order): MatchResult {
        val orderBook = getOrderBook(order.stockCode)
        val executions = mutableListOf<Execution>()
        var remainingQty = order.quantity

        // 매수 호가와 매칭 시도 (가격 조건: 매도가 <= 매수가)
        while (remainingQty > 0) {
            val bestBidPrice = orderBook.getBestBidPrice() ?: break

            // 매도 지정가보다 매수호가가 낮으면 매칭 불가
            if (order.price > bestBidPrice) break

            val bidOrder = orderBook.pollFirstBidOrderAt(bestBidPrice) ?: break

            // 체결 수량 결정
            val matchQty = minOf(remainingQty, bidOrder.remainingQuantity)
            val matchPrice = bidOrder.price // 먼저 들어온 주문 가격으로 체결

            // 체결 생성
            val execution =
                Execution(
                    executionId = snowflakeIdGenerator.nextId(),
                    orderNumber = order.orderNumber,
                    orderId = order.orderId,
                    stockCode = order.stockCode,
                    orderType = OrderType.SELL,
                    executedQuantity = matchQty,
                    executedPrice = matchPrice,
                )
            executions.add(execution)
            executionStore[execution.executionId] = execution

            logger.info(
                "[MATCH] 매도 체결: {} {}주 @ {}원 | sellOrder={}, bidOrder={}",
                order.stockCode,
                matchQty,
                matchPrice,
                order.orderNumber,
                bidOrder.orderNumber,
            )

            remainingQty -= matchQty

            // 매수 주문에 잔량이 있으면 다시 호가창에 추가
            val bidRemaining = bidOrder.remainingQuantity - matchQty
            if (bidRemaining > 0) {
                orderBook.addBidOrder(bidOrder.copy(remainingQuantity = bidRemaining))
            }
        }

        // 미체결 수량이 있으면 호가창에 추가
        if (remainingQty > 0) {
            val bookOrder =
                BookOrder(
                    orderNumber = order.orderNumber,
                    orderId = order.orderId,
                    price = order.price,
                    remainingQuantity = remainingQty,
                    originalQuantity = order.quantity,
                    orderTime = order.orderTime,
                )
            orderBook.addAskOrder(bookOrder)
            logger.info(
                "[BOOK] 매도 호가 추가: {} {}주 @ {}원 | orderNumber={}",
                order.stockCode,
                remainingQty,
                order.price,
                order.orderNumber,
            )
        }

        return MatchResult(executions, remainingQty)
    }

    /**
     * 주문 취소 (호가창에서 제거)
     */
    fun cancelOrder(
        stockCode: String,
        orderNumber: Long,
        orderType: OrderType,
    ): Boolean {
        val orderBook = getOrderBook(stockCode)
        val removed =
            when (orderType) {
                OrderType.BUY -> orderBook.removeBidOrder(orderNumber)
                OrderType.SELL -> orderBook.removeAskOrder(orderNumber)
            }

        if (removed != null) {
            logger.info("[CANCEL] 호가 취소: {} orderNumber={}", stockCode, orderNumber)
            return true
        }
        return false
    }

    /**
     * 체결 내역 조회
     */
    fun getExecutions(): List<Execution> = executionStore.values.toList()

    /**
     * 체결 건수
     */
    fun getExecutionCount(): Int = executionStore.size
}
