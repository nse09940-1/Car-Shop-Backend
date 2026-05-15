package main.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import contracts.events.EventEnvelope;
import contracts.events.EventTopics;
import contracts.events.EventType;
import contracts.events.OrderSentForApprovalPayload;
import contracts.events.OrderType;
import contracts.events.RequiredPartItem;
import contracts.events.StockCarOperationPayload;
import main.domain.exception.DomainValidationException;
import main.domain.order.CustomCarOrder;
import main.domain.order.StockCarOrder;
import main.infrastructure.persistence.entity.OutboxEventJpaEntity;
import main.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class OrderOutboxService {
    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final ObjectMapper objectMapper;
    private final TraceIdProvider traceIdProvider;

    public OrderOutboxService(OutboxEventJpaRepository outboxEventJpaRepository,
                              ObjectMapper objectMapper,
                              TraceIdProvider traceIdProvider) {
        this.outboxEventJpaRepository = Objects.requireNonNull(outboxEventJpaRepository, "outboxEventJpaRepository is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
        this.traceIdProvider = Objects.requireNonNull(traceIdProvider, "traceIdProvider is required");
    }

    public void enqueueStockCarReservationRequested(StockCarOrder order) {
        EventEnvelope<StockCarOperationPayload> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.STOCK_CAR_RESERVATION_REQUESTED,
                Instant.now(),
                traceIdProvider.currentOrCreate(),
                order.id(),
                OrderType.STOCK,
                new StockCarOperationPayload(order.carId(), null));
        saveEnvelope(envelope, order.id());
    }

    public void enqueueStockCarWriteOffRequested(StockCarOrder order) {
        EventEnvelope<StockCarOperationPayload> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.STOCK_CAR_WRITE_OFF_REQUESTED,
                Instant.now(),
                traceIdProvider.currentOrCreate(),
                order.id(),
                OrderType.STOCK,
                new StockCarOperationPayload(order.carId(), null));
        saveEnvelope(envelope, order.id());
    }

    public void enqueueStockCarReservationReleaseRequested(StockCarOrder order) {
        EventEnvelope<StockCarOperationPayload> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.STOCK_CAR_RESERVATION_RELEASE_REQUESTED,
                Instant.now(),
                traceIdProvider.currentOrCreate(),
                order.id(),
                OrderType.STOCK,
                new StockCarOperationPayload(order.carId(), null));
        saveEnvelope(envelope, order.id());
    }

    public void enqueueCustomCarPartsReservationRequested(CustomCarOrder order, List<RequiredPartItem> requiredPartItems) {
        EventEnvelope<OrderSentForApprovalPayload> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.CUSTOM_ORDER_PARTS_RESERVATION_REQUESTED,
                Instant.now(),
                traceIdProvider.currentOrCreate(),
                order.id(),
                OrderType.CUSTOM,
                new OrderSentForApprovalPayload(null, order.carModelId(), requiredPartItems));
        saveEnvelope(envelope, order.id());
    }

    public void enqueueCustomExecutionRequested(CustomCarOrder order) {
        EventEnvelope<Void> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.CUSTOM_ORDER_EXECUTION_REQUESTED,
                Instant.now(),
                traceIdProvider.currentOrCreate(),
                order.id(),
                OrderType.CUSTOM,
                null);
        saveEnvelope(envelope, order.id());
    }

    public void enqueueCustomReservationReleaseRequested(CustomCarOrder order) {
        EventEnvelope<Void> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.CUSTOM_ORDER_RESERVATION_RELEASE_REQUESTED,
                Instant.now(),
                traceIdProvider.currentOrCreate(),
                order.id(),
                OrderType.CUSTOM,
                null);
        saveEnvelope(envelope, order.id());
    }

    private void saveEnvelope(EventEnvelope<?> envelope, UUID aggregateId) {
        try {
            OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
            entity.setId(envelope.eventId());
            entity.setEventType(envelope.eventType().name());
            entity.setTopic(EventTopics.ORDER_EVENTS);
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
