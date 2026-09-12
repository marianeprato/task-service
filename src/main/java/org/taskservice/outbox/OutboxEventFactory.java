package org.taskservice.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.taskservice.correlation.CorrelationIdContext;
import org.taskservice.event.TaskCreatedEvent;
import org.taskservice.model.Task;

import java.time.Instant;
import java.util.UUID;

/**
 * Builds outbox rows from domain events. The correlation id is captured
 * here -- on the request thread, while it's still available via
 * {@link CorrelationIdContext} -- because by the time {@link OutboxRelay}
 * actually publishes this row to Kafka, it's running on a scheduler thread
 * where that ThreadLocal is empty.
 */
@Component
public class OutboxEventFactory {

    private final ObjectMapper objectMapper;

    public OutboxEventFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OutboxEventEntity forTaskCreated(Task task) {
        TaskCreatedEvent event = new TaskCreatedEvent(
                task.taskId(),
                task.taskTitle(),
                task.taskDescription(),
                task.taskDueDate(),
                task.priority(),
                Instant.now()
        );

        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateType("Task")
                .aggregateId(task.taskId())
                .eventType("TaskCreated")
                .payload(writeValueAsString(event))
                .correlationId(CorrelationIdContext.get())
                .createdAt(Instant.now())
                .build();
    }

    private String writeValueAsString(TaskCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize " + event.getClass().getSimpleName(), e);
        }
    }
}
