package com.kangfru.mocktradingsystem.domain

import java.math.BigDecimal
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 호가 정보
 */
data class PriceLevel(
    val price: BigDecimal,
    val quantity: Int,
    val orderCount: Int,
)

/**
 * 호가창 스냅샷 (API 응답용)
 */
data class OrderBookSnapshot(
    val stockCode: String,
    val askLevels: List<PriceLevel>, // 매도 호가 (낮은 가격순)
    val bidLevels: List<PriceLevel>, // 매수 호가 (높은 가격순)
    val bestAsk: BigDecimal?, // 최우선 매도호가
    val bestBid: BigDecimal?, // 최우선 매수호가
    val spread: BigDecimal?, // 스프레드
)

/**
 * 호가창에 대기 중인 주문
 */
data class BookOrder(
    val orderNumber: Long,
    val orderId: String,
    val price: BigDecimal,
    val remainingQuantity: Int,
    val originalQuantity: Int,
    val orderTime: java.time.LocalDateTime,
)

/**
 * 종목별 호가창
 * - 매도 호가: 가격 오름차순 (낮은 가격이 최우선)
 * - 매수 호가: 가격 내림차순 (높은 가격이 최우선)
 */
class OrderBook(
    val stockCode: String,
) {
    // 매도 호가: 가격 오름차순 (TreeMap 기본 정렬)
    private val askOrders = ConcurrentSkipListMap<BigDecimal, CopyOnWriteArrayList<BookOrder>>()

    // 매수 호가: 가격 내림차순
    private val bidOrders = ConcurrentSkipListMap<BigDecimal, CopyOnWriteArrayList<BookOrder>>(Comparator.reverseOrder())

    /**
     * 매도 주문 추가
     */
    fun addAskOrder(order: BookOrder) {
        askOrders.computeIfAbsent(order.price) { CopyOnWriteArrayList() }.add(order)
    }

    /**
     * 매수 주문 추가
     */
    fun addBidOrder(order: BookOrder) {
        bidOrders.computeIfAbsent(order.price) { CopyOnWriteArrayList() }.add(order)
    }

    /**
     * 최우선 매도호가의 주문들 조회
     */
    fun getBestAskOrders(): List<BookOrder>? {
        val entry = askOrders.firstEntry() ?: return null
        return entry.value.toList()
    }

    /**
     * 최우선 매수호가의 주문들 조회
     */
    fun getBestBidOrders(): List<BookOrder>? {
        val entry = bidOrders.firstEntry() ?: return null
        return entry.value.toList()
    }

    /**
     * 최우선 매도호가
     */
    fun getBestAskPrice(): BigDecimal? = askOrders.firstEntry()?.key

    /**
     * 최우선 매수호가
     */
    fun getBestBidPrice(): BigDecimal? = bidOrders.firstEntry()?.key

    /**
     * 매도 주문 제거
     */
    fun removeAskOrder(orderNumber: Long): BookOrder? {
        for ((price, orders) in askOrders) {
            val order = orders.find { it.orderNumber == orderNumber }
            if (order != null) {
                orders.remove(order)
                if (orders.isEmpty()) {
                    askOrders.remove(price)
                }
                return order
            }
        }
        return null
    }

    /**
     * 매수 주문 제거
     */
    fun removeBidOrder(orderNumber: Long): BookOrder? {
        for ((price, orders) in bidOrders) {
            val order = orders.find { it.orderNumber == orderNumber }
            if (order != null) {
                orders.remove(order)
                if (orders.isEmpty()) {
                    bidOrders.remove(price)
                }
                return order
            }
        }
        return null
    }

    /**
     * 특정 가격의 첫 번째 매도 주문 제거 및 반환
     */
    fun pollFirstAskOrderAt(price: BigDecimal): BookOrder? {
        val orders = askOrders[price] ?: return null
        if (orders.isEmpty()) return null
        val order = orders.removeAt(0)
        if (orders.isEmpty()) {
            askOrders.remove(price)
        }
        return order
    }

    /**
     * 특정 가격의 첫 번째 매수 주문 제거 및 반환
     */
    fun pollFirstBidOrderAt(price: BigDecimal): BookOrder? {
        val orders = bidOrders[price] ?: return null
        if (orders.isEmpty()) return null
        val order = orders.removeAt(0)
        if (orders.isEmpty()) {
            bidOrders.remove(price)
        }
        return order
    }

    /**
     * 호가창 스냅샷 생성 (5호가)
     */
    fun getSnapshot(levels: Int = 5): OrderBookSnapshot {
        val askLevels =
            askOrders.entries.take(levels).map { (price, orders) ->
                PriceLevel(
                    price = price,
                    quantity = orders.sumOf { it.remainingQuantity },
                    orderCount = orders.size,
                )
            }

        val bidLevels =
            bidOrders.entries.take(levels).map { (price, orders) ->
                PriceLevel(
                    price = price,
                    quantity = orders.sumOf { it.remainingQuantity },
                    orderCount = orders.size,
                )
            }

        val bestAsk = getBestAskPrice()
        val bestBid = getBestBidPrice()
        val spread = if (bestAsk != null && bestBid != null) bestAsk - bestBid else null

        return OrderBookSnapshot(
            stockCode = stockCode,
            askLevels = askLevels,
            bidLevels = bidLevels,
            bestAsk = bestAsk,
            bestBid = bestBid,
            spread = spread,
        )
    }
}
