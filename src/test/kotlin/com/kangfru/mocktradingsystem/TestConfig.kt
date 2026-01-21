package com.kangfru.mocktradingsystem

import com.kangfru.mocktradingsystem.domain.Order
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate

@TestConfiguration
class TestConfig {
    @Bean
    @Primary
    @Suppress("UNCHECKED_CAST")
    fun kafkaTemplate(): KafkaTemplate<String, Order> = Mockito.mock(KafkaTemplate::class.java) as KafkaTemplate<String, Order>
}
