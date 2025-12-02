package org.taskservice.dto;

import java.time.LocalDate;
import java.util.UUID;

import org.taskservice.model.Task;

import lombok.Builder;

@Builder
public record TaskResponse(
        UUID taskId, String taskTitle, String taskDescription, LocalDate taskCreationDate, LocalDate taskDueDate) {
    public static TaskResponse from(Task task) {
        return TaskResponse.builder()
                .taskId(task.taskId())
                .taskTitle(task.taskTitle())
                .taskDescription(task.taskDescription())
                .taskCreationDate(task.taskCreationDate())
                .taskDueDate(task.taskDueDate())
                .build();
    }
}
