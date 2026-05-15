package main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import contracts.events.EventTopics;
import contracts.events.EventType;
import contracts.events.OrderType;
import main.infrastructure.messaging.StorageOutboxPublisher;
import main.infrastructure.messaging.StorageOutboxService;
import main.infrastructure.messaging.TraceIdProvider;
import main.infrastructure.persistence.entity.OutboxEventJpaEntity;
import main.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxTest {

    @Test
    void storageOutboxService_savesOrderApprovedEnvelope() throws Exception {
        OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
        TraceIdProvider traceIdProvider = mock(TraceIdProvider.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        StorageOutboxService service = new StorageOutboxService(repository, objectMapper, traceIdProvider);

        UUID orderId = UUID.randomUUID();
        UUID assemblyOrderId = UUID.randomUUID();
        when(traceIdProvider.currentOrCreate()).thenReturn("trace-approve");

        service.enqueueApproved(orderId, OrderType.CUSTOM, assemblyOrderId);

        var captor = org.mockito.ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventJpaEntity entity = captor.getValue();

        assertEquals(EventType.CUSTOM_ORDER_PARTS_RESERVATION_COMPLETED.name(), entity.getEventType());
        assertEquals(EventTopics.STORAGE_EVENTS, entity.getTopic());
        assertEquals(orderId, entity.getAggregateId());
        assertEquals("trace-approve", entity.getTraceId());
        assertFalse(entity.isPublished());
        assertFalse(entity.isRemoved());

        JsonNode root = objectMapper.readTree(entity.getPayload());
        assertEquals(entity.getId().toString(), root.get("eventId").asText());
        assertEquals(EventType.CUSTOM_ORDER_PARTS_RESERVATION_COMPLETED.name(), root.get("eventType").asText());
        assertEquals(orderId.toString(), root.get("orderId").asText());
        assertEquals("CUSTOM", root.get("orderType").asText());
        assertEquals(assemblyOrderId.toString(), root.get("payload").get("assemblyOrderId").asText());
    }

    @Test
    void storageOutboxService_savesStockWriteOffRejectedEnvelope() throws Exception {
        OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
        TraceIdProvider traceIdProvider = mock(TraceIdProvider.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        StorageOutboxService service = new StorageOutboxService(repository, objectMapper, traceIdProvider);

        UUID orderId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        when(traceIdProvider.currentOrCreate()).thenReturn("trace-stock");

        service.enqueueStockCarWriteOffRejected(orderId, carId, "No reservation");

        var captor = org.mockito.ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventJpaEntity entity = captor.getValue();

        assertEquals(EventType.STOCK_CAR_WRITE_OFF_REJECTED.name(), entity.getEventType());
        assertEquals(EventTopics.STORAGE_EVENTS, entity.getTopic());
        assertEquals(orderId, entity.getAggregateId());
        assertEquals("trace-stock", entity.getTraceId());
        assertFalse(entity.isPublished());

        JsonNode root = objectMapper.readTree(entity.getPayload());
        assertEquals("STOCK", root.get("orderType").asText());
        assertEquals(carId.toString(), root.get("payload").get("carId").asText());
        assertEquals("No reservation", root.get("payload").get("reason").asText());
    }

    @Test
    void storageOutboxService_savesCustomReleaseCompletedEnvelope() throws Exception {
        OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
        TraceIdProvider traceIdProvider = mock(TraceIdProvider.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        StorageOutboxService service = new StorageOutboxService(repository, objectMapper, traceIdProvider);

        UUID orderId = UUID.randomUUID();
        UUID assemblyOrderId = UUID.randomUUID();
        when(traceIdProvider.currentOrCreate()).thenReturn("trace-custom-release");

        service.enqueueCustomReservationReleaseCompleted(orderId, assemblyOrderId);

        var captor = org.mockito.ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventJpaEntity entity = captor.getValue();

        assertEquals(EventType.CUSTOM_ORDER_RESERVATION_RELEASE_COMPLETED.name(), entity.getEventType());

        JsonNode root = objectMapper.readTree(entity.getPayload());
        assertEquals(EventType.CUSTOM_ORDER_RESERVATION_RELEASE_COMPLETED.name(), root.get("eventType").asText());
        assertEquals("CUSTOM", root.get("orderType").asText());
        assertEquals(assemblyOrderId.toString(), root.get("payload").get("assemblyOrderId").asText());
    }

    @Test
    void storageOutboxService_savesStockReleaseRejectedEnvelope() throws Exception {
        OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
        TraceIdProvider traceIdProvider = mock(TraceIdProvider.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        StorageOutboxService service = new StorageOutboxService(repository, objectMapper, traceIdProvider);

        UUID orderId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        when(traceIdProvider.currentOrCreate()).thenReturn("trace-stock-release");

        service.enqueueStockCarReservationReleaseRejected(orderId, carId, "Already written off");

        var captor = org.mockito.ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventJpaEntity entity = captor.getValue();

        assertEquals(EventType.STOCK_CAR_RESERVATION_RELEASE_REJECTED.name(), entity.getEventType());

        JsonNode root = objectMapper.readTree(entity.getPayload());
        assertEquals(EventType.STOCK_CAR_RESERVATION_RELEASE_REJECTED.name(), root.get("eventType").asText());
        assertEquals("STOCK", root.get("orderType").asText());
        assertEquals(carId.toString(), root.get("payload").get("carId").asText());
        assertEquals("Already written off", root.get("payload").get("reason").asText());
    }

    @Test
    void storageOutboxPublisher_marksPublishedOnSuccessfulKafkaAck() {
        OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        StorageOutboxPublisher publisher = new StorageOutboxPublisher(repository, kafkaTemplate);

        OutboxEventJpaEntity event = pendingEvent("storage.events", UUID.randomUUID(), "{\"ok\":true}");
        when(repository.findPendingForPublish(20)).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publishPendingEvents();

        assertTrue(event.isPublished());
        assertNotNull(event.getPublishedAt());
        verify(repository).save(event);
        verify(kafkaTemplate).send(eq(event.getTopic()), eq(event.getAggregateId().toString()), eq(event.getPayload()));
    }

    @Test
    void storageOutboxPublisher_stopsBatchAfterFirstFailedSend() {
        OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        StorageOutboxPublisher publisher = new StorageOutboxPublisher(repository, kafkaTemplate);

        OutboxEventJpaEntity first = pendingEvent("storage.events", UUID.randomUUID(), "{\"n\":1}");
        OutboxEventJpaEntity second = pendingEvent("storage.events", UUID.randomUUID(), "{\"n\":2}");
        when(repository.findPendingForPublish(20)).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(eq(first.getTopic()), eq(first.getAggregateId().toString()), eq(first.getPayload())))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka down")));

        publisher.publishPendingEvents();

        verify(repository, never()).save(any(OutboxEventJpaEntity.class));
        verify(kafkaTemplate, times(1))
                .send(eq(first.getTopic()), eq(first.getAggregateId().toString()), eq(first.getPayload()));
        verify(kafkaTemplate, never())
                .send(eq(second.getTopic()), eq(second.getAggregateId().toString()), eq(second.getPayload()));
        assertFalse(first.isPublished());
        assertFalse(second.isPublished());
    }

    private static OutboxEventJpaEntity pendingEvent(String topic, UUID aggregateId, String payload) {
        OutboxEventJpaEntity event = new OutboxEventJpaEntity();
        event.setId(UUID.randomUUID());
        event.setEventType(EventType.CUSTOM_ORDER_PARTS_RESERVATION_COMPLETED.name());
        event.setTopic(topic);
        event.setAggregateId(aggregateId);
        event.setTraceId("trace");
        event.setPayload(payload);
        event.setPublished(false);
        event.setRemoved(false);
        return event;
    }
}
