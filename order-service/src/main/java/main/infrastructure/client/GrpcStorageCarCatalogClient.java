package main.infrastructure.client;

import contracts.grpc.AvailableCarResponse;
import contracts.grpc.GetAvailableCarRequest;
import contracts.grpc.ListAvailableCarsRequest;
import contracts.grpc.StorageCarServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PreDestroy;
import main.application.dto.AvailableCarDto;
import main.application.port.client.StorageCarCatalogClient;
import main.domain.Money;
import main.domain.exception.EntityNotFoundException;
import main.domain.exception.StorageServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class GrpcStorageCarCatalogClient implements StorageCarCatalogClient {
    private static final Logger log = LoggerFactory.getLogger(GrpcStorageCarCatalogClient.class);

    private final ManagedChannel channel;
    private final StorageCarServiceGrpc.StorageCarServiceBlockingStub stub;
    private final long timeoutMs;

    @Autowired
    public GrpcStorageCarCatalogClient(
            @Value("${app.storage.grpc.host:localhost}") String host,
            @Value("${app.storage.grpc.port:9090}") int port,
            @Value("${app.storage.grpc.timeout-ms:1500}") long timeoutMs) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build(), timeoutMs);
        log.info("Storage gRPC client configured target={}:{} timeoutMs={}", host, port, timeoutMs);
    }

    GrpcStorageCarCatalogClient(ManagedChannel channel, long timeoutMs) {
        this.channel = channel;
        this.stub = StorageCarServiceGrpc.newBlockingStub(channel);
        this.timeoutMs = timeoutMs;
    }

    @Override
    public List<AvailableCarDto> findAvailableCars() {
        long startedAt = System.nanoTime();
        try {
            var response = stub.withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS)
                    .listAvailableCars(ListAvailableCarsRequest.newBuilder().build());
            log.info("gRPC listAvailableCars completed count={} durationMs={}", response.getCarsCount(), elapsedMs(startedAt));
            return response.getCarsList().stream().map(this::toDto).toList();
        } catch (StatusRuntimeException ex) {
            log.warn("gRPC listAvailableCars failed status={} durationMs={}", ex.getStatus().getCode(), elapsedMs(startedAt));
            throw mapException(ex, "Storage service is unavailable");
        }
    }

    @Override
    public AvailableCarDto findAvailableCar(UUID id) {
        long startedAt = System.nanoTime();
        try {
            var response = stub.withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS)
                    .getAvailableCar(GetAvailableCarRequest.newBuilder().setId(id.toString()).build());
            log.info("gRPC getAvailableCar completed carId={} durationMs={}", id, elapsedMs(startedAt));
            return toDto(response);
        } catch (StatusRuntimeException ex) {
            log.warn("gRPC getAvailableCar failed carId={} status={} durationMs={}", id, ex.getStatus().getCode(), elapsedMs(startedAt));
            throw mapException(ex, "Storage service is unavailable");
        }
    }

    @PreDestroy
    public void shutdown() {
        channel.shutdown();
    }

    private RuntimeException mapException(StatusRuntimeException ex, String unavailableMessage) {
        Status.Code code = ex.getStatus().getCode();
        if (code == Status.Code.NOT_FOUND) {
            return new EntityNotFoundException("Available car not found");
        }
        if (code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED) {
            return new StorageServiceUnavailableException(unavailableMessage);
        }
        return new StorageServiceUnavailableException(unavailableMessage);
    }

    private AvailableCarDto toDto(AvailableCarResponse response) {
        return new AvailableCarDto(
                UUID.fromString(response.getId()),
                response.getVin(),
                UUID.fromString(response.getCarModelId()),
                response.getColor(),
                new Money(response.getPriceRubles()),
                response.getAvailableForTestDrive());
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
