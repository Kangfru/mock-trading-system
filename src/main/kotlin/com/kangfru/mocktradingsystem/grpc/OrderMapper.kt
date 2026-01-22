package com.kangfru.mocktradingsystem.grpc

import com.google.protobuf.timestamp
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import com.kangfru.mocktradingsystem.domain.Order as DomainOrder
import com.kangfru.mocktradingsystem.domain.OrderAction as DomainOrderAction
import com.kangfru.mocktradingsystem.domain.OrderStatus as DomainOrderStatus
import com.kangfru.mocktradingsystem.domain.OrderType as DomainOrderType
import com.kangfru.mocktradingsystem.domain.PriceType as DomainPriceType

fun DomainOrder.toProto(): Order =
    order {
        orderId = this@toProto.orderId
        orderNumber = this@toProto.orderNumber
        stockCode = this@toProto.stockCode
        orderType = when (this@toProto.orderType) {
            DomainOrderType.BUY -> OrderType.ORDER_TYPE_BUY
            DomainOrderType.SELL -> OrderType.ORDER_TYPE_SELL
        }
        quantity = this@toProto.quantity
        price = this@toProto.price.toPlainString()
        priceType = when (this@toProto.priceType) {
            DomainPriceType.LIMIT -> PriceType.PRICE_TYPE_LIMIT
            DomainPriceType.MARKET -> PriceType.PRICE_TYPE_MARKET
        }
        orderTime = timestamp {
            nanos = this@toProto.orderTime.nano
            seconds = this@toProto.orderTime.toEpochSecond(ZoneOffset.of("+09:00"))
        }
        status = when (this@toProto.status) {
            DomainOrderStatus.PENDING -> OrderStatus.ORDER_STATUS_PENDING
            DomainOrderStatus.FILLED -> OrderStatus.ORDER_STATUS_FILLED
            DomainOrderStatus.CANCELLED -> OrderStatus.ORDER_STATUS_CANCELLED
            DomainOrderStatus.MODIFIED -> OrderStatus.ORDER_STATUS_MODIFIED
        }
        action = when (this@toProto.action) {
            DomainOrderAction.NEW -> OrderAction.ORDER_ACTION_NEW
            DomainOrderAction.CANCEL -> OrderAction.ORDER_ACTION_CANCEL
            DomainOrderAction.MODIFY -> OrderAction.ORDER_ACTION_MODIFY
        }
        this@toProto.originalOrderNumber?.let {
            originalOrderNumber = it
        }
    }

fun Order.toDomain(): DomainOrder =
    DomainOrder(
        orderId = this.orderId,
        orderNumber = this.orderNumber,
        stockCode = this.stockCode,
        orderType = when (this.orderType) {
            OrderType.ORDER_TYPE_BUY -> DomainOrderType.BUY
            OrderType.ORDER_TYPE_SELL -> DomainOrderType.SELL
            else -> DomainOrderType.BUY
        },
        quantity = this.quantity,
        price = BigDecimal(this.price),
        priceType = when (this.priceType) {
            PriceType.PRICE_TYPE_LIMIT -> DomainPriceType.LIMIT
            PriceType.PRICE_TYPE_MARKET -> DomainPriceType.MARKET
            else -> DomainPriceType.LIMIT
        },
        orderTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(this.orderTime.seconds, this.orderTime.nanos.toLong()),
            ZoneId.of("Asia/Seoul")
        ),
        status = when (this.status) {
            OrderStatus.ORDER_STATUS_PENDING -> DomainOrderStatus.PENDING
            OrderStatus.ORDER_STATUS_FILLED -> DomainOrderStatus.FILLED
            OrderStatus.ORDER_STATUS_CANCELLED -> DomainOrderStatus.CANCELLED
            OrderStatus.ORDER_STATUS_MODIFIED -> DomainOrderStatus.MODIFIED
            else -> DomainOrderStatus.PENDING
        },
        action = when (this.action) {
            OrderAction.ORDER_ACTION_NEW -> DomainOrderAction.NEW
            OrderAction.ORDER_ACTION_CANCEL -> DomainOrderAction.CANCEL
            OrderAction.ORDER_ACTION_MODIFY -> DomainOrderAction.MODIFY
            else -> DomainOrderAction.NEW
        },
        originalOrderNumber = if (this.hasOriginalOrderNumber()) this.originalOrderNumber else null
    )


