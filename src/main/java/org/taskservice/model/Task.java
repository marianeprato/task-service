package org.taskservice.model;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;

@Builder
public record Task(
        UUID taskId,
        String taskTitle,
        String taskDescription,
        LocalDate taskCreationDate,
        LocalDate taskDueDate,
        TaskPriority priority) {}
