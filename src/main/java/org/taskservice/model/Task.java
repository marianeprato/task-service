package org.taskservice.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record Task(UUID taskId, String taskTitle, String taskDescription,
                   LocalDate taskCreationDate, LocalDate taskDueDate) {
    public Task {
        Objects.requireNonNull(taskId, "Task ID cannot be null");
        Objects.requireNonNull(taskTitle, "Task title cannot be null");
        Objects.requireNonNull(taskDescription, "Task description cannot be null");
        Objects.requireNonNull(taskCreationDate, "Creation date cannot be null");
        Objects.requireNonNull(taskDueDate, "Due date cannot be null");
    }
}
