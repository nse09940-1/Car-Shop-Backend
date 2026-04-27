package main.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import contracts.events.EventType;
import contracts.events.OrderType;
import main.application.service.OrderService;
import main.infrastructure.persistence.entity.ProcessedEventJpaEntity;
import main.infrastructure.persistence.repository.ProcessedEventJpaRepository;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class StorageDecisionConsumer {
    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final ProcessedEventJpaRepository processedEventJpaRepository;

    public StorageDecisionConsumer(ObjectMapper objectMapper,
                                   OrderService orderService,
                                   ProcessedEventJpaRepository processedEventJpaRepository) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
        this.orderService = Objects.requireNonNull(orderService, "orderService is required");
        this.processedEventJpaRepository = Objects.requireNonNull(processedEventJpaRepository, "incomingEventJpaRepository is required");
    }

    @KafkaListener(topics = "#{T(contracts.events.EventTopics).STORAGE_EVENTS_V1}", groupId = "${app.kafka.consumer-group}")
    @Transactional
    public void onMessage(String message) throws Exception {
        JsonNode root = objectMapper.readTree(message);
        UUID eventId = UUID.fromString(root.get("eventId").asText());
        if (processedEventJpaRepository.existsById(eventId)) {
            return;
        }

        String traceId = root.get("traceId").asText();
        MDC.put(TraceIdProvider.TRACE_ID_KEY, traceId);
        try {
            EventType eventType = EventType.valueOf(root.get("eventType").asText());
            UUID orderId = UUID.fromString(root.get("orderId").asText());
            OrderType orderType = OrderType.valueOf(root.get("orderType").asText());
            if (!processEvent(eventType, orderId, orderType)) {
                return;
            }

            ProcessedEventJpaEntity entity = new ProcessedEventJpaEntity();
            entity.setId(eventId);
            entity.setEventType(eventType.name());
            entity.setTraceId(traceId);
            entity.setOrderId(orderId);
            entity.setProcessedAt(Instant.now());
            processedEventJpaRepository.save(entity);
        } finally {
            MDC.remove(TraceIdProvider.TRACE_ID_KEY);
        }
    }

    private boolean processEvent(EventType eventType, UUID orderId, OrderType orderType) {
        if (orderType == OrderType.STOCK) {
            if (eventType == EventType.STOCK_CAR_RESERVED) {
                orderService.markStockCarReserved(orderId);
                return true;
            }
            if (eventType == EventType.STOCK_CAR_RESERVATION_REJECTED) {
                orderService.rejectStockCarReservation(orderId);
                return true;
            }
            if (eventType == EventType.STOCK_CAR_WRITTEN_OFF) {
                orderService.markStockCarWrittenOff(orderId);
                return true;
            }
            if (eventType == EventType.STOCK_CAR_WRITE_OFF_REJECTED) {
                orderService.rejectStockCarWriteOff(orderId);
                return true;
            }
            return false;
        }

        if (eventType == EventType.ORDER_APPROVED) {
            orderService.approveCustomOrder(orderId);
            return true;
        }
        if (eventType == EventType.ORDER_REJECTED) {
            orderService.rejectCustomOrderOnCreate(orderId);
            return true;
        }
        if (eventType == EventType.ORDER_EXECUTION_STARTED) {
            orderService.markCustomOrderAwaitingDelivery(orderId);
            return true;
        }
        return false;
    }
}
