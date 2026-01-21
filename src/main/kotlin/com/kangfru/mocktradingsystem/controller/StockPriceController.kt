package com.kangfru.mocktradingsystem.controller

import com.kangfru.mocktradingsystem.client.StooqClient
import com.kangfru.mocktradingsystem.domain.StockQuote
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/stocks")
class StockPriceController(
    private val stooqClient: StooqClient,
) {
    @GetMapping("/{symbol}/quote")
    fun getQuote(
        @PathVariable symbol: String,
        @RequestParam(defaultValue = "us") market: String,
    ): ResponseEntity<StockQuote> {
        val quote = stooqClient.getQuote(symbol, market)
        return if (quote != null) {
            ResponseEntity.ok(quote)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
