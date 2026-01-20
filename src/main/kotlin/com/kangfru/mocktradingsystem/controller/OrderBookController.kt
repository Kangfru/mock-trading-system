package com.kangfru.mocktradingsystem.controller

import com.kangfru.mocktradingsystem.domain.OrderBookSnapshot
import com.kangfru.mocktradingsystem.service.OrderBookService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orderbook")
class OrderBookController(
    private val orderBookService: OrderBookService
) {

    /**
     * 특정 종목 호가창 조회
     */
    @GetMapping("/{stockCode}")
    fun getOrderBook(@PathVariable stockCode: String): OrderBookSnapshot {
        return orderBookService.getOrderBookSnapshot(stockCode)
    }

    /**
     * 모든 종목 호가창 조회
     */
    @GetMapping
    fun getAllOrderBooks(): Map<String, OrderBookSnapshot> {
        return orderBookService.getAllOrderBookSnapshots()
    }
}
