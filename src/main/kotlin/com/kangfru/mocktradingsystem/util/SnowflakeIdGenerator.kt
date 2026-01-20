package com.kangfru.mocktradingsystem.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
class SnowflakeIdGenerator(
    @Value("\${app.snowflake.node-id:1}") private val nodeId: Long
) {
    companion object {
        private const val EPOCH = 1704067200000L // 2024-01-01 00:00:00 UTC
        private const val NODE_ID_BITS = 10
        private const val SEQUENCE_BITS = 12

        private const val MAX_NODE_ID = (1L shl NODE_ID_BITS) - 1
        private const val MAX_SEQUENCE = (1L shl SEQUENCE_BITS) - 1

        private const val NODE_ID_SHIFT = SEQUENCE_BITS
        private const val TIMESTAMP_SHIFT = SEQUENCE_BITS + NODE_ID_BITS
    }

    private val lock = ReentrantLock()
    private var lastTimestamp = -1L
    private var sequence = 0L

    init {
        require(nodeId in 0..MAX_NODE_ID) { "Node ID must be between 0 and $MAX_NODE_ID" }
    }

    fun nextId(): Long = lock.withLock {
        var timestamp = currentTimeMillis()

        if (timestamp < lastTimestamp) {
            throw IllegalStateException("Clock moved backwards. Refusing to generate id.")
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) and MAX_SEQUENCE
            if (sequence == 0L) {
                timestamp = waitNextMillis(lastTimestamp)
            }
        } else {
            sequence = 0L
        }

        lastTimestamp = timestamp

        ((timestamp - EPOCH) shl TIMESTAMP_SHIFT) or
                (nodeId shl NODE_ID_SHIFT) or
                sequence
    }

    private fun currentTimeMillis(): Long = System.currentTimeMillis()

    private fun waitNextMillis(lastTimestamp: Long): Long {
        var timestamp = currentTimeMillis()
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis()
        }
        return timestamp
    }

    fun parseId(id: Long): SnowflakeInfo {
        val timestamp = (id shr TIMESTAMP_SHIFT) + EPOCH
        val nodeId = (id shr NODE_ID_SHIFT) and MAX_NODE_ID
        val sequence = id and MAX_SEQUENCE
        return SnowflakeInfo(timestamp, nodeId, sequence)
    }

    data class SnowflakeInfo(
        val timestamp: Long,
        val nodeId: Long,
        val sequence: Long
    )
}
