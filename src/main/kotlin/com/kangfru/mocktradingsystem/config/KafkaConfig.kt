package com.kangfru.mocktradingsystem.config

import com.kangfru.mocktradingsystem.domain.Order
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import org.springframework.kafka.support.serializer.JacksonJsonSerializer

@Configuration
@EnableKafka
class KafkaConfig {
    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.consumer.group-id}")
    private lateinit var groupId: String

    // Producer 튜닝 설정
    @Value("\${app.kafka.producer.batch-size:16384}")
    private var batchSize: Int = 16384

    @Value("\${app.kafka.producer.linger-ms:5}")
    private var lingerMs: Int = 5

    @Value("\${app.kafka.producer.buffer-memory:33554432}")
    private var bufferMemory: Long = 33554432

    @Value("\${app.kafka.producer.compression-type:lz4}")
    private lateinit var compressionType: String

    @Value("\${app.kafka.producer.acks:1}")
    private lateinit var acks: String

    // Consumer 튜닝 설정
    @Value("\${app.kafka.consumer.fetch-min-bytes:1}")
    private var fetchMinBytes: Int = 1

    @Value("\${app.kafka.consumer.fetch-max-wait-ms:500}")
    private var fetchMaxWaitMs: Int = 500

    @Value("\${app.kafka.consumer.max-poll-records:500}")
    private var maxPollRecords: Int = 500

    @Value("\${app.kafka.consumer.concurrency:3}")
    private var concurrency: Int = 3

    @Bean
    fun producerFactory(): ProducerFactory<String, Order> {
        val configProps =
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JacksonJsonSerializer::class.java,
                // 튜닝 설정
                ProducerConfig.BATCH_SIZE_CONFIG to batchSize,
                ProducerConfig.LINGER_MS_CONFIG to lingerMs,
                ProducerConfig.BUFFER_MEMORY_CONFIG to bufferMemory,
                ProducerConfig.COMPRESSION_TYPE_CONFIG to compressionType,
                ProducerConfig.ACKS_CONFIG to acks,
            )
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Order> = KafkaTemplate(producerFactory())

    @Bean
    fun consumerFactory(): ConsumerFactory<String, Order> {
        val configProps =
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to groupId,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JacksonJsonDeserializer::class.java,
                JacksonJsonDeserializer.TRUSTED_PACKAGES to "com.kangfru.mocktradingsystem.domain",
                // 튜닝 설정
                ConsumerConfig.FETCH_MIN_BYTES_CONFIG to fetchMinBytes,
                ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG to fetchMaxWaitMs,
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG to maxPollRecords,
            )
        return DefaultKafkaConsumerFactory(
            configProps,
            StringDeserializer(),
            JacksonJsonDeserializer(Order::class.java),
        )
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Order> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Order>()
        factory.setConsumerFactory(consumerFactory())
        factory.setConcurrency(concurrency) // 병렬 Consumer 수
        factory.containerProperties.ackMode = ContainerProperties.AckMode.BATCH // 배치 ACK
        return factory
    }
}
