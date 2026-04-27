package main.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import contracts.events.EventEnvelope;
import contracts.events.EventTopics;
import contracts.events.EventType;
import contracts.events.OrderApprovedPayload;
import contracts.events.OrderRejectedPayload;
import contracts.events.OrderType;
import contracts.events.StockCarOperationPayload;
import main.domain.exception.DomainValidationException;
import main.infrastructure.persistence.entity.OutboxEventJpaEntity;
import main.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class StorageOutboxService {
    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final ObjectMapper objectMapper;
    private final TraceIdProvider traceIdProvider;

    public StorageOutboxService(OutboxEventJpaRepository outboxEventJpaRepository,
                                ObjectMapper objectMapper,
                                TraceIdProvider traceIdProvider) {
        this.outboxEventJpaRepository = Objects.requireNonNull(outboxEventJpaRepository, "outboxEventJpaRepository is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
        this.traceIdProvider = Objects.requireNonNull(traceIdProvider, "traceIdProvider is required");
    }

    public void enqueueApproved(UUID orderId, OrderType orderType, UUID assemblyOrderId) {
        EventEnvelope<OrderApprovedPayload> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.ORDER_APPROVED,
                Instant.now(),
                traceIdProvider.currentOrCreate(),
                orderId,
                orderType,
                new OrderApprovedPayload(assemblyOrderId));
        save(envelope, orderId);
    }

    public void enqueueRejected(UUID orderId, OrderType orderType, UUID assemblyOrderId, String reason) {
        EventEnvelope<OrderRejectedPayload> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.ORDER_REJECTED,
                Instant.now(),
                traceIdProvider.currentOrCreate(),
                orderId,
                orderType,
                new OrderRejectedPayload(assemblyOrderId, reason));
        save(envelope, orderId);
    }

    public void enqueueExecutionStarted(UUID orderId, OrderType orderType, UUID assemblyOrderId) {
        EventEnvelope<OrderApprovedPayload> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.ORDER_EXECUTION_STARTED,
                Instant.now(),
                traceIdProvider.currentOrCreate(),
                orderId,
                orderType,
                new OrderApprovedPayload(assemblyOrderId));
        save(envelope, orderId);
    }

    public void enqueueStockCarReserved(UUID orderId, UUID carId) {
        EventEnvelope<StockCarOperationPayload> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.STOCK_CAR_RESERVED,
                Instant.now(),
                traceIdProvider.currentOrCreate(),
                orderId,
                OrderType.STOCK,
                new StockCarOperationPayload(carId, null));
        save(envelope, orderId);
    }

    public void enqueueStockCarReservationRejected(UUID orderId, UUID carId, String reason) {
        EventEnvelope<StockCarOperationPayload> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.STOCK_CAR_RESERVATION_REJECTED,
                Instant.now(),
                traceIdProvider.currentOrCreate(),
                orderId,
                OrderType.STOCK,
                new StockCarOperationPayload(carId, reason));
        save(envelope, orderId);
    }

    public void enqueueStockCarWrittenOff(UUID orderId, UUID carId) {
        EventEnvelope<StockCarOperationPayload> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.STOCK_CAR_WRITTEN_OFF,
                Instant.now(),
                traceIdProvider.currentOrCreate(),
                orderId,
                OrderType.STOCK,
                new StockCarOperationPayload(carId, null));
        save(envelope, orderId);
    }

    public void enqueueStockCarWriteOffRejected(UUID orderId, UUID carId, String reason) {
        EventEnvelope<StockCarOperationPayload> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.STOCK_CAR_WRITE_OFF_REJECTED,
                Instant.now(),
                traceIdProvider.currentOrCreate(),
                orderId,
                OrderType.STOCK,
                new StockCarOperationPayload(carId, reason));
        save(envelope, orderId);
    }

    private void save(EventEnvelope<?> envelope, UUID aggregateId) {
        try {
            OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
            entity.setId(envelope.eventId());
            entity.setEventType(envelope.eventType().name());
            entity.setTopic(EventTopics.STORAGE_EVENTS);
            entity.setAggregateId(aggregateId);
            entity.setTraceId(envelope.traceId());
            entity.setPayload(objectMapper.writeValueAsString(envelope));
            entity.setPublished(false);
            entity.setRemoved(false);
            outboxEventJpaRepository.save(entity);
        } catch (JsonProcessingException ex) {
            throw new DomainValidationException("Cannot serialize outbox event");
        }
    }
}
