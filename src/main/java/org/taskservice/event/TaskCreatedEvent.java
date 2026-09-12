package org.taskservice.event;

import org.taskservice.model.TaskPriority;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Wire contract published to the "task-created" Kafka topic. Intentionally
 * decoupled from {@link org.taskservice.model.Task}: reminder-service owns
 * its own copy of this shape and the two are never allowed to diverge from
 * a shared Java type across the two repos.
 */
public record TaskCreatedEvent(
        UUID taskId,
        String taskTitle,
        String taskDescription,
        LocalDate taskDueDate,
        TaskPriority priority,
        Instant occurredAt
) {}
