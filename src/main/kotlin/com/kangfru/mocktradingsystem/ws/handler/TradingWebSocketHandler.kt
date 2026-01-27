package com.kangfru.mocktradingsystem.ws.handler

import com.kangfru.mocktradingsystem.domain.Order
import com.kangfru.mocktradingsystem.grpc.toDomain
import com.kangfru.mocktradingsystem.grpc.toStockHoldingDto
import com.kangfru.mocktradingsystem.service.AccountService
import com.kangfru.mocktradingsystem.service.ExecutionService
import com.kangfru.mocktradingsystem.ws.message.WsCancelOrderRequest
import com.kangfru.mocktradingsystem.ws.message.WsCreateOrderRequest
import com.kangfru.mocktradingsystem.ws.message.WsErrorResponse
import com.kangfru.mocktradingsystem.ws.message.WsGetPortfolioRequest
import com.kangfru.mocktradingsystem.ws.message.WsOrderCancelledResponse
import com.kangfru.mocktradingsystem.ws.message.WsOrderCreatedResponse
import com.kangfru.mocktradingsystem.ws.message.WsOrderQueriedResponse
import com.kangfru.mocktradingsystem.ws.message.WsPortfolioResponse
import com.kangfru.mocktradingsystem.ws.message.WsQueryOrderRequest
import com.kangfru.mocktradingsystem.ws.message.WsRequest
import com.kangfru.mocktradingsystem.ws.message.WsResponse
import com.kangfru.mocktradingsystem.ws.message.toDto
import com.kangfru.mocktradingsystem.ws.ratelimit.RateLimiterManager
import com.kangfru.mocktradingsystem.ws.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import tools.jackson.databind.ObjectMapper

@Component
class TradingWebSocketHandler(
    private val rateLimiterManager: RateLimiterManager,
    private val executionService: ExecutionService,
    private val accountService: AccountService,
    private val objectMapper: ObjectMapper,
    private val sessionManager: SessionManager,
) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(javaClass)
    override fun handle(session: WebSocketSession): Mono<Void> {
        sessionManager.addSession(session)
        logger.info("session connected : ${session.id}")
        val output: Flux<WebSocketMessage> = session.receive()
            .map { objectMapper.readValue(it.payloadAsText, WsRequest::class.java) }
            .flatMap { request -> processRequest(session, request) }
            .map { response ->
                session.textMessage(objectMapper.writeValueAsString(response))
            }

        return session.send(output)
            .doFinally {
                sessionManager.removeSession(session.id)
                rateLimiterManager.removeSession(session.id)
                logger.info("session disconnected : ${session.id}")
            }
    }

    private fun processRequest(session: WebSocketSession, request: WsRequest): Mono<WsResponse> {
        // Rate Limit
        if (!rateLimiterManager.tryAcquire(session.id)) {
            return Mono.just(WsErrorResponse(
                requestId = request.requestId,
                errorCode = HttpStatus.TOO_MANY_REQUESTS.name,
                errorMessage = HttpStatus.TOO_MANY_REQUESTS.reasonPhrase,
            ))
        }

        return when (request) {
            is WsCreateOrderRequest -> handleCreateOrder(request)
            is WsQueryOrderRequest -> handleQueryOrder(request)
            is WsCancelOrderRequest -> handleCancelOrder(request)
            is WsGetPortfolioRequest -> handleGetPortfolio(request)
        }
    }

    private fun handleCreateOrder(request: WsCreateOrderRequest): Mono<WsResponse> {
        return Mono.fromCallable {
            val order = request.toDomain()
            // 2. 주문 처리
            executionService.processOrder(order)
            order.orderNumber  // 생성된 주문번호 반환
        }
        .subscribeOn(Schedulers.boundedElastic())
        .map<WsResponse> { orderNumber ->
            WsOrderCreatedResponse(
                requestId = request.requestId,
                orderNumber = orderNumber
            )
        }
        .onErrorResume { e ->
            // 4. 에러 응답
            Mono.just<WsResponse>(WsErrorResponse(
                requestId = request.requestId,
                errorCode = "ORDER_FAILED",
                errorMessage = e.message ?: "주문 처리 실패"
            ))
        }
    }

    private fun handleQueryOrder(request: WsQueryOrderRequest): Mono<WsResponse> {
        return Mono.fromCallable {
            val order = executionService.getOrder(request.orderNumber)
            order
        }
            .subscribeOn(Schedulers.boundedElastic())
            .map<WsResponse> { order ->
                WsOrderQueriedResponse(
                    requestId = request.requestId,
                    order = order.toDto()
                )
            }
            .onErrorResume { e ->
                // 4. 에러 응답
                Mono.just<WsResponse>(WsErrorResponse(
                    requestId = request.requestId,
                    errorCode = "QUERY_FAILED",
                    errorMessage = e.message ?: "주문 조회 실패"
                ))
            }
    }

    private fun handleCancelOrder(request: WsCancelOrderRequest): Mono<WsResponse> {
        return Mono.fromCallable {
            executionService.processOrder(
                Order(
                    orderNumber = request.orderNumber,
                    originalOrderNumber = request.originalOrderNumber,
                    stockCode = request.stockCode,
                    orderType = request.orderType,
                    quantity = request.quantity,
                    price = request.price
                )
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .map<WsResponse> {
                WsOrderCancelledResponse(
                    requestId = request.requestId,
                    orderNumber = request.orderNumber
                )
            }
            .onErrorResume { e ->
                // 4. 에러 응답
                Mono.just<WsResponse>(
                    WsErrorResponse(
                        requestId = request.requestId,
                        errorCode = "ORDER_QUERY_FAILED",
                        errorMessage = e.message ?: "주문 조회 실패"
                    )
                )
            }
    }


    private fun handleGetPortfolio(request: WsGetPortfolioRequest): Mono<WsResponse> {
        return mono(Dispatchers.IO) {
            accountService.getHoldings(request.accountNumber)
        }
            .subscribeOn(Schedulers.boundedElastic())
            .map<WsResponse> { stockHoldings ->
                WsPortfolioResponse(
                    requestId = request.requestId,
                    accountNumber = request.accountNumber,
                    holdings = stockHoldings.map { it.value.toStockHoldingDto() }
                )
            }
            .onErrorResume { e ->
                // 4. 에러 응답
                Mono.just<WsResponse>(
                    WsErrorResponse(
                        requestId = request.requestId,
                        errorCode = "PORTFOLIO_QUERY_FAILED",
                        errorMessage = e.message ?: "포트폴리오 조회 실패"
                    )
                )
            }
    }
}