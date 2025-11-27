package org.taskservice.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.taskservice.model.TaskPriority;

@Builder
public record CreateTaskRequest(
        @NotNull @Size(min = 3, max = 100) String taskTitle,
        @NotNull @Size(min = 3, max = 500) String taskDescription,
        @FutureOrPresent @NotNull LocalDate taskDueDate,
        TaskPriority priority
) {}

