package main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import contracts.events.EventTopics;
import contracts.events.EventType;
import main.domain.Money;
import main.domain.configuration.CarConfiguration;
import main.domain.order.CustomCarOrder;
import main.domain.order.CustomOrderStatus;
import main.domain.order.StockCarOrder;
import main.domain.order.StockOrderStatus;
import main.infrastructure.messaging.OrderOutboxPublisher;
import main.infrastructure.messaging.OrderOutboxService;
import main.infrastructure.messaging.TraceIdProvider;
import main.infrastructure.persistence.entity.OutboxEventJpaEntity;
import main.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
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
    void orderOutboxService_savesStockReservationEnvelope() throws Exception {
        OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
        TraceIdProvider traceIdProvider = mock(TraceIdProvider.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        OrderOutboxService service = new OrderOutboxService(repository, objectMapper, traceIdProvider);

        UUID orderId = UUID.randomUUID();
        StockCarOrder order = new StockCarOrder(
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.now(),
                UUID.randomUUID(),
                StockOrderStatus.CREATED
        );
        when(traceIdProvider.currentOrCreate()).thenReturn("trace-stock");

        service.enqueueStockCarReservationRequested(order);

        var captor = org.mockito.ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventJpaEntity entity = captor.getValue();

        assertEquals(EventType.STOCK_CAR_RESERVATION_REQUESTED.name(), entity.getEventType());
        assertEquals(EventTopics.ORDER_EVENTS, entity.getTopic());
        assertEquals(orderId, entity.getAggregateId());
        assertEquals("trace-stock", entity.getTraceId());
        assertFalse(entity.isPublished());
        assertFalse(entity.isRemoved());

        JsonNode root = objectMapper.readTree(entity.getPayload());
        assertEquals(entity.getId().toString(), root.get("eventId").asText());
        assertEquals(EventType.STOCK_CAR_RESERVATION_REQUESTED.name(), root.get("eventType").asText());
        assertEquals(orderId.toString(), root.get("orderId").asText());
        assertEquals("STOCK", root.get("orderType").asText());
        assertEquals(order.carId().toString(), root.get("payload").get("carId").asText());
    }

    @Test
    void orderOutboxService_savesCustomApprovalEnvelopeWithParts() throws Exception {
        OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
        TraceIdProvider traceIdProvider = mock(TraceIdProvider.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        OrderOutboxService service = new OrderOutboxService(repository, objectMapper, traceIdProvider);

        UUID orderId = UUID.randomUUID();
        UUID modelId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        CustomCarOrder order = new CustomCarOrder(
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.now(),
                modelId,
                new CarConfiguration(null, null, null, null),
                new Money(1_000_000),
                CustomOrderStatus.CREATED
        );
        when(traceIdProvider.currentOrCreate()).thenReturn("trace-custom");

        service.enqueueCustomCarPartsReservationRequested(order, List.of(new contracts.events.RequiredPartItem(partId, 2)));

        var captor = org.mockito.ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventJpaEntity entity = captor.getValue();

        assertEquals(EventType.CUSTOM_ORDER_PARTS_RESERVATION_REQUESTED.name(), entity.getEventType());
        assertEquals(EventTopics.ORDER_EVENTS, entity.getTopic());
        assertEquals(orderId, entity.getAggregateId());
        assertEquals("trace-custom", entity.getTraceId());
        assertFalse(entity.isPublished());

        JsonNode root = objectMapper.readTree(entity.getPayload());
        assertEquals("CUSTOM", root.get("orderType").asText());
        assertEquals(modelId.toString(), root.get("payload").get("carModelId").asText());
        assertEquals(partId.toString(), root.get("payload").get("requiredParts").get(0).get("partId").asText());
        assertEquals(2, root.get("payload").get("requiredParts").get(0).get("quantity").asInt());
    }

    @Test
    void orderOutboxService_savesStockReservationReleaseEnvelope() throws Exception {
        OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
        TraceIdProvider traceIdProvider = mock(TraceIdProvider.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        OrderOutboxService service = new OrderOutboxService(repository, objectMapper, traceIdProvider);

        UUID orderId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        StockCarOrder order = new StockCarOrder(
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.now(),
                carId,
                StockOrderStatus.MANAGER_APPROVED
        );
        when(traceIdProvider.currentOrCreate()).thenReturn("trace-release");

        service.enqueueStockCarReservationReleaseRequested(order);

        var captor = org.mockito.ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventJpaEntity entity = captor.getValue();

        assertEquals(EventType.STOCK_CAR_RESERVATION_RELEASE_REQUESTED.name(), entity.getEventType());
        assertEquals(EventTopics.ORDER_EVENTS, entity.getTopic());

        JsonNode root = objectMapper.readTree(entity.getPayload());
        assertEquals(EventType.STOCK_CAR_RESERVATION_RELEASE_REQUESTED.name(), root.get("eventType").asText());
        assertEquals("STOCK", root.get("orderType").asText());
        assertEquals(carId.toString(), root.get("payload").get("carId").asText());
    }

    @Test
    void orderOutboxPublisher_marksPublishedOnSuccessfulKafkaAck() {
        OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        OrderOutboxPublisher publisher = new OrderOutboxPublisher(repository, kafkaTemplate);

        OutboxEventJpaEntity event = pendingEvent("order.events", UUID.randomUUID(), "{\"ok\":true}");
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
    void orderOutboxPublisher_stopsBatchAfterFirstFailedSend() {
        OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        OrderOutboxPublisher publisher = new OrderOutboxPublisher(repository, kafkaTemplate);

        OutboxEventJpaEntity first = pendingEvent("order.events", UUID.randomUUID(), "{\"n\":1}");
        OutboxEventJpaEntity second = pendingEvent("order.events", UUID.randomUUID(), "{\"n\":2}");
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
        event.setEventType(EventType.CUSTOM_ORDER_PARTS_RESERVATION_REQUESTED.name());
        event.setTopic(topic);
        event.setAggregateId(aggregateId);
        event.setTraceId("trace");
        event.setPayload(payload);
        event.setPublished(false);
        event.setRemoved(false);
        return event;
    }
}
