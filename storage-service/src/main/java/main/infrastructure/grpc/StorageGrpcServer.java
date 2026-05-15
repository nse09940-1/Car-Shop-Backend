package main.infrastructure.grpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class StorageGrpcServer implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(StorageGrpcServer.class);

    private final StorageCarGrpcService storageCarGrpcService;
    private final int configuredPort;
    private Server server;
    private boolean running;

    public StorageGrpcServer(StorageCarGrpcService storageCarGrpcService,
                             @Value("${app.grpc.port:9090}") int configuredPort) {
        this.storageCarGrpcService = storageCarGrpcService;
        this.configuredPort = configuredPort;
    }

    @Override
    public void start() {
        try {
            server = NettyServerBuilder.forPort(configuredPort)
                    .addService(storageCarGrpcService)
                    .build()
                    .start();
            running = true;
            log.info("Storage gRPC server started on port {}", getPort());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot start Storage gRPC server", ex);
        }
    }

    @Override
    public void stop() {
        if (server == null) {
            running = false;
            return;
        }
        try {
            server.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            running = false;
            log.info("Storage gRPC server stopped");
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return server == null ? configuredPort : server.getPort();
    }
}
