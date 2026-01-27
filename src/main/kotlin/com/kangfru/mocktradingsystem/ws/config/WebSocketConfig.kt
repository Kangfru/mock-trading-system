package com.kangfru.mocktradingsystem.ws.config

import com.kangfru.mocktradingsystem.ws.handler.TradingWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping

@Configuration
class WebSocketConfig {

    @Bean
    fun webSocketHandlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }

    @Bean
    fun handlerMapping(tradingHandler: TradingWebSocketHandler): HandlerMapping {
        val urlMap: Map<String, TradingWebSocketHandler> = mapOf(
            "/ws/trading" to tradingHandler
        )
        return SimpleUrlHandlerMapping(urlMap, 1)
    }

    @Bean
    fun corsWebFilter(): CorsWebFilter {
        val config = CorsConfiguration().apply {
            allowCredentials = true
            addAllowedOriginPattern(("*"))
            addAllowedHeader(("*"))
            addAllowedMethod(("*"))
        }

        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/ws/**", config)
        }

        return CorsWebFilter(source)
    }

}