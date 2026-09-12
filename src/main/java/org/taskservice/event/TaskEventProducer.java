package org.taskservice.event;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.taskservice.correlation.CorrelationIdContext;
import org.taskservice.model.Task;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class TaskEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventProducer.class);

    private final KafkaTemplate<String, TaskCreatedEvent> kafkaTemplate;
    private final String taskCreatedTopic;

    public TaskEventProducer(KafkaTemplate<String, TaskCreatedEvent> kafkaTemplate,
                              @Value("${app.kafka.topic.task-created}") String taskCreatedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.taskCreatedTopic = taskCreatedTopic;
    }

    public void publishTaskCreated(Task task) {
        TaskCreatedEvent event = new TaskCreatedEvent(
                task.taskId(),
                task.taskTitle(),
                task.taskDescription(),
                task.taskDueDate(),
                task.priority(),
                Instant.now()
        );

        ProducerRecord<String, TaskCreatedEvent> record =
                new ProducerRecord<>(taskCreatedTopic, task.taskId().toString(), event);
        String correlationId = CorrelationIdContext.get();
        if (correlationId != null) {
            record.headers().add(new RecordHeader(
                    CorrelationIdContext.HEADER_NAME, correlationId.getBytes(StandardCharsets.UTF_8)));
        }

        try {
            kafkaTemplate.send(record)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.warn("Failed to publish TaskCreated event for taskId={}: {}",
                                    task.taskId(), exception.toString());
                        } else {
                            log.info("Published TaskCreated event for taskId={} to topic {} (offset={})",
                                    task.taskId(), taskCreatedTopic, result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            // KafkaTemplate#send can also throw synchronously (e.g. topic metadata
            // unreachable within max.block.ms) rather than failing the returned future.
            // Task creation must not fail just because reminder delivery is degraded.
            log.warn("Failed to publish TaskCreated event for taskId={}: {}", task.taskId(), e.toString());
        }
    }
}
