package com.kangfru.mocktradingsystem.producer

import com.kangfru.mocktradingsystem.domain.Order
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class OrderProducer(
    private val kafkaTemplate: KafkaTemplate<String, Order>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${app.kafka.topic.order}")
    private lateinit var orderTopic: String

    fun sendOrder(order: Order) {
        logger.info("Sending order: $order")
        kafkaTemplate.send(orderTopic, order.orderId, order)
    }

    fun sendOrders(orders: List<Order>) {
        orders.forEach { sendOrder(it) }
        logger.info("Sent ${orders.size} orders")
    }
}
