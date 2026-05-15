package main.infrastructure.client;

import contracts.grpc.GetCarStateRequest;
import contracts.grpc.StorageCarServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PreDestroy;
import main.application.port.client.StorageReadClient;
import main.domain.exception.EntityNotFoundException;
import main.domain.exception.StorageServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class GrpcStorageReadClient implements StorageReadClient {
    private static final Logger log = LoggerFactory.getLogger(GrpcStorageReadClient.class);

    private final ManagedChannel channel;
    private final StorageCarServiceGrpc.StorageCarServiceBlockingStub stub;
    private final long timeoutMs;

    @Autowired
    public GrpcStorageReadClient(
            @Value("${app.storage.grpc.host:localhost}") String host,
            @Value("${app.storage.grpc.port:9090}") int port,
            @Value("${app.storage.grpc.timeout-ms:1500}") long timeoutMs) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build(), timeoutMs);
        log.info("Storage read gRPC client configured target={}:{} timeoutMs={}", host, port, timeoutMs);
    }

    GrpcStorageReadClient(ManagedChannel channel, long timeoutMs) {
        this.channel = channel;
        this.stub = StorageCarServiceGrpc.newBlockingStub(channel);
        this.timeoutMs = timeoutMs;
    }

    @Override
    public StorageCarSnapshot getCar(UUID carId) {
        long startedAt = System.nanoTime();
        try {
            var response = stub.withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS)
                    .getCarState(GetCarStateRequest.newBuilder().setId(carId.toString()).build());
            log.info("gRPC getCarState completed carId={} durationMs={}", carId, elapsedMs(startedAt));
            return new StorageCarSnapshot(
                    UUID.fromString(response.getId()),
                    response.getAvailable(),
                    response.getAvailableForTestDrive());
        } catch (StatusRuntimeException ex) {
            log.warn("gRPC getCarState failed carId={} status={} durationMs={}",
                    carId, ex.getStatus().getCode(), elapsedMs(startedAt));
            throw mapException(ex);
        }
    }

    @PreDestroy
    public void shutdown() {
        channel.shutdown();
    }

    private RuntimeException mapException(StatusRuntimeException ex) {
        Status.Code code = ex.getStatus().getCode();
        if (code == Status.Code.NOT_FOUND) {
            return new EntityNotFoundException("Car not found");
        }
        if (code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED) {
            return new StorageServiceUnavailableException("Storage service is unavailable");
        }
        return new StorageServiceUnavailableException("Storage service is unavailable");
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
