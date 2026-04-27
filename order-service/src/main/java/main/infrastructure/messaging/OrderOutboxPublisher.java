package main.infrastructure.messaging;

import main.infrastructure.persistence.entity.OutboxEventJpaEntity;
import main.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
public class OrderOutboxPublisher {
    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderOutboxPublisher(OutboxEventJpaRepository outboxEventJpaRepository,
                                KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventJpaRepository = Objects.requireNonNull(outboxEventJpaRepository, "outboxEventJpaRepository is required");
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate is required");
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventJpaEntity> events = outboxEventJpaRepository.findPendingForPublish(20);
        for (OutboxEventJpaEntity event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload()).get();
                event.setPublished(true);
                event.setPublishedAt(Instant.now());
                outboxEventJpaRepository.save(event);
            } catch (Exception ex) {
                break;
            }
        }
    }
}
