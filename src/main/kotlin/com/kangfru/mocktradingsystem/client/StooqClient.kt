package com.kangfru.mocktradingsystem.client

import com.kangfru.mocktradingsystem.domain.StockQuote
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
class StooqClient(
    private val webClient: WebClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val lock = ReentrantLock()
    private var lastRequestTime: Long = 0
    private val minIntervalMs: Long = 1000 // 최소 1초 간격

    fun getQuote(
        symbol: String,
        market: String = "us",
    ): StockQuote? {
        throttle()

        val fullSymbol = "${symbol.lowercase()}.$market"
        logger.info("[STOOQ] 시세 조회 요청: {}", fullSymbol)

        return try {
            val response =
                webClient
                    .get()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/q/l/")
                            .queryParam("s", fullSymbol)
                            .queryParam("f", "sd2t2ohlcv")
                            .queryParam("h")
                            .queryParam("e", "csv")
                            .build()
                    }.retrieve()
                    .bodyToMono(String::class.java)
                    .block()

            parseResponse(response)
        } catch (e: Exception) {
            logger.error("[STOOQ] 시세 조회 실패: {}", fullSymbol, e)
            null
        }
    }

    private fun throttle() {
        lock.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < minIntervalMs) {
                val sleepTime = minIntervalMs - elapsed
                logger.debug("[STOOQ] Throttling: {}ms 대기", sleepTime)
                Thread.sleep(sleepTime)
            }
            lastRequestTime = System.currentTimeMillis()
        }
    }

    private fun parseResponse(csv: String?): StockQuote? {
        if (csv.isNullOrBlank()) return null

        val lines = csv.trim().lines()
        if (lines.size < 2) return null

        val dataLine = lines[1]
        val parts = dataLine.split(",")

        if (parts.size < 8) return null

        // N/D는 데이터 없음을 의미
        if (parts[1] == "N/D") {
            logger.warn("[STOOQ] 데이터 없음: {}", parts[0])
            return null
        }

        return try {
            StockQuote(
                symbol = parts[0],
                date = LocalDate.parse(parts[1], DateTimeFormatter.ISO_LOCAL_DATE),
                time = LocalTime.parse(parts[2], DateTimeFormatter.ofPattern("HH:mm:ss")),
                open = BigDecimal(parts[3]),
                high = BigDecimal(parts[4]),
                low = BigDecimal(parts[5]),
                close = BigDecimal(parts[6]),
                volume = parts[7].toLong(),
            )
        } catch (e: Exception) {
            logger.error("[STOOQ] CSV 파싱 실패: {}", dataLine, e)
            null
        }
    }
}
