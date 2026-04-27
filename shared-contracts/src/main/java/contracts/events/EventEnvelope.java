package contracts.events;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventEnvelope<T>(
        UUID eventId,
        EventType eventType,
        Instant occurredAt,
        String traceId,
        UUID orderId,
        OrderType orderType,
        T payload) {
}
