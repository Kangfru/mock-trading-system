package com.kangfru.mocktradingsystem.config

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class WebClientConfig {
    @Bean
    fun webClient(): WebClient {
        // 개발용: SSL 검증 비활성화
        val sslContext =
            SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build()

        val httpClient =
            HttpClient
                .create()
                .secure { it.sslContext(sslContext) }

        return WebClient
            .builder()
            .baseUrl("https://stooq.com")
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }
}
