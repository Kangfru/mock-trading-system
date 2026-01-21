package com.kangfru.mocktradingsystem.controller

import com.kangfru.mocktradingsystem.client.StooqClient
import com.kangfru.mocktradingsystem.domain.*
import com.kangfru.mocktradingsystem.producer.OrderProducer
import com.kangfru.mocktradingsystem.util.SnowflakeIdGenerator
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import kotlin.random.Random

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderProducer: OrderProducer,
    private val snowflakeIdGenerator: SnowflakeIdGenerator,
    private val executionService: com.kangfru.mocktradingsystem.service.ExecutionService,
    private val stooqClient: StooqClient,
) {
    private val stockCodes = listOf("AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "META", "NVDA", "NFLX")

    @PostMapping
    fun createOrder(
        @RequestBody request: CreateOrderRequest,
    ): Order {
        val price =
            when (request.priceType) {
                PriceType.MARKET -> {
                    // 시장가 주문: 현재 시세 조회
                    val quote =
                        stooqClient.getQuote(request.stockCode)
                            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "시세 조회 실패: ${request.stockCode}")
                    quote.close
                }
                PriceType.LIMIT -> {
                    // 지정가 주문: 가격 필수
                    request.price
                        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지정가 주문은 가격이 필수입니다")
                }
            }

        val order =
            Order(
                orderNumber = snowflakeIdGenerator.nextId(),
                stockCode = request.stockCode,
                orderType = request.orderType,
                quantity = request.quantity,
                price = price,
                priceType = request.priceType,
                action = OrderAction.NEW,
            )
        orderProducer.sendOrder(order)
        return order
    }

    @PostMapping("/cancel")
    fun cancelOrder(
        @RequestBody request: CancelOrderRequest,
    ): Order {
        val cancelOrder =
            Order(
                orderNumber = snowflakeIdGenerator.nextId(),
                stockCode = "", // 취소 시에는 종목코드 불필요
                orderType = OrderType.BUY, // placeholder
                quantity = 0,
                price = BigDecimal.ZERO,
                action = OrderAction.CANCEL,
                originalOrderNumber = request.originalOrderNumber,
            )
        orderProducer.sendOrder(cancelOrder)
        return cancelOrder
    }

    @PostMapping("/modify")
    fun modifyOrder(
        @RequestBody request: ModifyOrderRequest,
    ): Order {
        val modifyOrder =
            Order(
                orderNumber = snowflakeIdGenerator.nextId(),
                stockCode = "", // 정정 시에는 종목코드 불필요
                orderType = OrderType.BUY, // placeholder
                quantity = request.newQuantity ?: 0,
                price = request.newPrice ?: BigDecimal.ZERO,
                action = OrderAction.MODIFY,
                originalOrderNumber = request.originalOrderNumber,
            )
        orderProducer.sendOrder(modifyOrder)
        return modifyOrder
    }

    @PostMapping("/bulk")
    fun createBulkOrders(
        @RequestParam(defaultValue = "100") count: Int,
    ): Map<String, Any> {
        val orders = generateRandomOrders(count)
        orderProducer.sendOrders(orders)
        return mapOf(
            "message" to "Successfully sent $count orders",
            "count" to count,
        )
    }

    @PostMapping("/bulk/mixed")
    fun createMixedBulkOrders(
        @RequestParam(defaultValue = "100") newCount: Int,
        @RequestParam(defaultValue = "20") cancelCount: Int,
        @RequestParam(defaultValue = "30") modifyCount: Int,
    ): Map<String, Any> {
        // 1. 신규 주문 생성
        val newOrders = generateRandomOrders(newCount)
        orderProducer.sendOrders(newOrders)

        // 2. 일부 주문에 대해 취소 요청 생성
        val ordersToCancel = newOrders.shuffled().take(cancelCount.coerceAtMost(newCount))
        val cancelOrders =
            ordersToCancel.map { original ->
                Order(
                    orderNumber = snowflakeIdGenerator.nextId(),
                    stockCode = "",
                    orderType = OrderType.BUY,
                    quantity = 0,
                    price = BigDecimal.ZERO,
                    action = OrderAction.CANCEL,
                    originalOrderNumber = original.orderNumber,
                )
            }
        orderProducer.sendOrders(cancelOrders)

        // 3. 일부 주문에 대해 정정 요청 생성
        val ordersToModify =
            newOrders.filterNot { ordersToCancel.contains(it) }.shuffled().take(
                modifyCount.coerceAtMost(
                    newCount - cancelCount,
                ),
            )
        val modifyOrders =
            ordersToModify.map { original ->
                Order(
                    orderNumber = snowflakeIdGenerator.nextId(),
                    stockCode = "",
                    orderType = OrderType.BUY,
                    quantity = Random.nextInt(1, 500),
                    price = BigDecimal.valueOf(Random.nextDouble(10.0, 500.0)).setScale(2, java.math.RoundingMode.HALF_UP),
                    action = OrderAction.MODIFY,
                    originalOrderNumber = original.orderNumber,
                )
            }
        orderProducer.sendOrders(modifyOrders)

        return mapOf(
            "message" to "Successfully sent mixed orders",
            "newOrders" to newCount,
            "cancelOrders" to cancelOrders.size,
            "modifyOrders" to modifyOrders.size,
            "total" to (newCount + cancelOrders.size + modifyOrders.size),
        )
    }

    @GetMapping("/stats")
    fun getStats(): Map<String, Any> = executionService.getStats()

    private fun generateRandomOrders(count: Int): List<Order> =
        (1..count).map {
            Order(
                orderNumber = snowflakeIdGenerator.nextId(),
                stockCode = stockCodes.random(),
                orderType = OrderType.entries.toTypedArray().random(),
                quantity = Random.nextInt(1, 1000),
                price = BigDecimal.valueOf(Random.nextDouble(10.0, 500.0)).setScale(2, java.math.RoundingMode.HALF_UP),
                action = OrderAction.NEW,
            )
        }
}
