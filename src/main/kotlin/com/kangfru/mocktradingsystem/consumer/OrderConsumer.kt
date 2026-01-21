package com.kangfru.mocktradingsystem.consumer

import com.kangfru.mocktradingsystem.domain.Order
import com.kangfru.mocktradingsystem.domain.OrderAction
import com.kangfru.mocktradingsystem.service.ExecutionService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class OrderConsumer(
    private val executionService: ExecutionService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${app.kafka.topic.order}"],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun consumeOrder(order: Order) {
        when (order.action) {
            OrderAction.NEW ->
                logger.info(
                    "Received [NEW]: orderNumber={}, [{}] {} - {}주 @ {}원",
                    order.orderNumber,
                    order.orderType,
                    order.stockCode,
                    order.quantity,
                    order.price,
                )
            OrderAction.CANCEL ->
                logger.info(
                    "Received [CANCEL]: orderNumber={}, originalOrderNumber={}",
                    order.orderNumber,
                    order.originalOrderNumber,
                )
            OrderAction.MODIFY ->
                logger.info(
                    "Received [MODIFY]: orderNumber={}, originalOrderNumber={}, newQty={}, newPrice={}",
                    order.orderNumber,
                    order.originalOrderNumber,
                    order.quantity,
                    order.price,
                )
        }

        executionService.processOrder(order)
    }
}
