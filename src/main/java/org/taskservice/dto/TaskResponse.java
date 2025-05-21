package org.taskservice.dto;

import org.taskservice.model.Task;

import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID taskId,
        String taskTitle,
        String taskDescription,
        LocalDate taskCreationDate,
        LocalDate taskDueDate
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.taskId(),
                task.taskTitle(),
                task.taskDescription(),
                task.taskCreationDate(),
                task.taskDueDate()
        );
    }
}
