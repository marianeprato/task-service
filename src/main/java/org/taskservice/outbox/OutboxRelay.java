package org.taskservice.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Polling publisher for the transactional outbox: periodically reads
 * unpublished rows and publishes each to Kafka, marking it published only
 * after a successful send. A row that fails to send is simply left
 * unpublished and retried on the next poll -- at-least-once delivery, so
 * the Kafka consumer on the other end must tolerate (or dedupe) duplicates.
 *
 * This is a polling publisher rather than CDC (e.g. Debezium reading the
 * DB's write-ahead log): simpler to run and reason about for this project's
 * scale, at the cost of poll-interval latency and a periodic query against
 * the outbox table. CDC removes both of those but adds a whole separate
 * piece of infrastructure to run and operate.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final String taskCreatedTopic;

    public OutboxRelay(OutboxEventRepository outboxEventRepository,
                        KafkaEventPublisher kafkaEventPublisher,
                        @Value("${app.kafka.topic.task-created}") String taskCreatedTopic) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaEventPublisher = kafkaEventPublisher;
        this.taskCreatedTopic = taskCreatedTopic;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.poll-interval-ms:2000}")
    public void publishPendingEvents() {
        List<OutboxEventEntity> pending = outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEventEntity event : pending) {
            publish(event);
        }
    }

    @Transactional
    void publish(OutboxEventEntity event) {
        try {
            kafkaEventPublisher.publish(
                    taskCreatedTopic, event.getAggregateId().toString(), event.getPayload(), event.getCorrelationId());
            event.setPublishedAt(Instant.now());
            outboxEventRepository.save(event);
            log.info("Published outbox event id={} aggregateId={} to topic {}",
                    event.getId(), event.getAggregateId(), taskCreatedTopic);
        } catch (Exception e) {
            log.warn("Failed to publish outbox event id={}, will retry on next poll: {}",
                    event.getId(), e.toString());
        }
    }
}
