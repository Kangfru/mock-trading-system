package com.kangfru.mocktradingsystem.grpc

import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionServiceV1
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class GrpcServerStarter(
    private val grpcOrderService: GrpcOrderService,
    @Value("\${grpc.server.port}")
    private val port: Int
) : DisposableBean {

    private var server: Server? = null
    private val logger = LoggerFactory.getLogger(GrpcServerStarter::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun startGrpcServer() {
        server = ServerBuilder.forPort(port)
            .addService(grpcOrderService)
            .addService(ProtoReflectionServiceV1.newInstance())
            .build()
            .start()
        logger.info("gRPC server started on port $port")
    }

    override fun destroy() {
        server?.shutdown()
        server?.awaitTermination()
    }
}