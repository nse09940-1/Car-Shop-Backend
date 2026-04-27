package main.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import contracts.events.EventType;
import contracts.events.OrderSentForApprovalPayload;
import contracts.events.OrderType;
import contracts.events.StockCarOperationPayload;
import main.application.service.AssemblyOrderService;
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
public class OrderApprovalRequestConsumer {
    private final ObjectMapper objectMapper;
    private final AssemblyOrderService assemblyOrderService;
    private final StorageOutboxService storageOutboxService;
    private final ProcessedEventJpaRepository processedEventJpaRepository;

    public OrderApprovalRequestConsumer(ObjectMapper objectMapper,
                                        AssemblyOrderService assemblyOrderService,
                                        StorageOutboxService storageOutboxService,
                                        ProcessedEventJpaRepository processedEventJpaRepository) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
        this.assemblyOrderService = Objects.requireNonNull(assemblyOrderService, "assemblyOrderService is required");
        this.storageOutboxService = Objects.requireNonNull(storageOutboxService, "storageOutboxService is required");
        this.processedEventJpaRepository = Objects.requireNonNull(processedEventJpaRepository, "incomingEventJpaRepository is required");
    }

    @KafkaListener(topics = "#{T(contracts.events.EventTopics).ORDER_EVENTS_V1}", groupId = "${app.kafka.consumer-group}")
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
            if (eventType == EventType.ORDER_SENT_FOR_APPROVAL) {
                OrderSentForApprovalPayload payload = objectMapper.treeToValue(root.get("payload"), OrderSentForApprovalPayload.class);
                AssemblyOrderService.ProcessingResult result = assemblyOrderService.processApprovalRequest(orderId, orderType, payload);

                if (result.approved()) {
                    storageOutboxService.enqueueApproved(orderId, orderType, result.assemblyOrderId());
                } else {
                    storageOutboxService.enqueueRejected(orderId, orderType, result.assemblyOrderId(), result.reason());
                }
            } else if (eventType == EventType.ORDER_EXECUTION_REQUESTED) {
                AssemblyOrderService.ProcessingResult result = assemblyOrderService.processExecutionRequest(orderId, orderType);
                storageOutboxService.enqueueExecutionStarted(orderId, orderType, result.assemblyOrderId());
            } else if (eventType == EventType.ORDER_RESERVATION_RELEASE_REQUESTED) {
                assemblyOrderService.processReservationRelease(orderId, orderType);
            } else if (eventType == EventType.STOCK_CAR_RESERVATION_REQUESTED) {
                StockCarOperationPayload payload = objectMapper.treeToValue(root.get("payload"), StockCarOperationPayload.class);
                AssemblyOrderService.ProcessingResult result = assemblyOrderService.processStockReservationRequest(orderId, payload);
                if (result.approved()) {
                    storageOutboxService.enqueueStockCarReserved(orderId, payload.carId());
                } else {
                    storageOutboxService.enqueueStockCarReservationRejected(orderId, payload.carId(), result.reason());
                }
            } else if (eventType == EventType.STOCK_CAR_WRITE_OFF_REQUESTED) {
                StockCarOperationPayload payload = objectMapper.treeToValue(root.get("payload"), StockCarOperationPayload.class);
                AssemblyOrderService.ProcessingResult result = assemblyOrderService.processStockWriteOffRequest(orderId, payload);
                if (result.approved()) {
                    storageOutboxService.enqueueStockCarWrittenOff(orderId, payload.carId());
                } else {
                    storageOutboxService.enqueueStockCarWriteOffRejected(orderId, payload.carId(), result.reason());
                }
            } else {
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
}
