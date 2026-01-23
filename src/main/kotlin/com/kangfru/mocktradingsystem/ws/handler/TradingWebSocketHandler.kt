package com.kangfru.mocktradingsystem.ws.handler

import com.kangfru.mocktradingsystem.grpc.toDomain
import com.kangfru.mocktradingsystem.service.AccountService
import com.kangfru.mocktradingsystem.service.ExecutionService
import com.kangfru.mocktradingsystem.ws.message.WsCancelOrderRequest
import com.kangfru.mocktradingsystem.ws.message.WsCreateOrderRequest
import com.kangfru.mocktradingsystem.ws.message.WsErrorResponse
import com.kangfru.mocktradingsystem.ws.message.WsGetPortfolioRequest
import com.kangfru.mocktradingsystem.ws.message.WsOrderCreatedResponse
import com.kangfru.mocktradingsystem.ws.message.WsQueryOrderRequest
import com.kangfru.mocktradingsystem.ws.message.WsRequest
import com.kangfru.mocktradingsystem.ws.message.WsResponse
import com.kangfru.mocktradingsystem.ws.ratelimit.RateLimiterManager
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
    private val objectMapper: ObjectMapper
) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(javaClass)
    override fun handle(session: WebSocketSession): Mono<Void> {
        logger.info("session connected : ${session.id}")
        val output: Flux<WebSocketMessage> = session.receive()
            .map { objectMapper.readValue(it.payloadAsText, WsRequest::class.java) }
            .flatMap { request -> processRequest(session, request) }
            .map { response ->
                session.textMessage(objectMapper.writeValueAsString(response))
            }

        return session.send(output)
            .doFinally {
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
        TODO()
    }

    private fun handleCancelOrder(request: WsCancelOrderRequest): Mono<WsResponse> {
        TODO()
    }

    private fun handleGetPortfolio(request: WsGetPortfolioRequest): Mono<WsResponse> {
        TODO()
    }
}