package org.taskservice.model;

import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record Task(
        UUID taskId,
        String taskTitle,
        String taskDescription,
        LocalDate taskCreationDate,
        LocalDate taskDueDate,
        TaskPriority priority
) {}
