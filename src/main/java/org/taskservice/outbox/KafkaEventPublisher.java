package org.taskservice.outbox;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.taskservice.correlation.CorrelationIdContext;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Thin Kafka-sending wrapper used exclusively by {@link OutboxRelay}. It
 * publishes an already-serialized JSON payload (read back from an outbox
 * row) rather than a domain object, so it has no dependency on any
 * particular event's Java type.
 */
@Component
public class KafkaEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends synchronously (bounded by a timeout) since this is called from a
     * background relay poll, not a request thread -- there's no caller
     * waiting on a response to serve, so blocking here just keeps the
     * relay's "publish, then mark published" logic simple and sequential.
     */
    public void publish(String topic, String key, String payloadJson, String correlationId) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payloadJson);
        if (correlationId != null) {
            record.headers().add(new RecordHeader(
                    CorrelationIdContext.HEADER_NAME, correlationId.getBytes(StandardCharsets.UTF_8)));
        }
        try {
            kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new KafkaPublishException("Failed to publish event to topic " + topic, e);
        }
    }

    public static class KafkaPublishException extends RuntimeException {
        public KafkaPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
