package org.taskservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.taskservice.dto.CreateTaskRequest;
import org.taskservice.dto.ReminderResponse;
import org.taskservice.dto.TaskResponse;
import org.taskservice.exception.TaskValidationException;
import org.taskservice.service.TaskService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/tasks")
@Tag(name = "Tasks", description = "Create and list tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "List all tasks")
    public ResponseEntity<List<TaskResponse>> getTasks() {
        log.info("Get all tasks request received");
        final List<TaskResponse> list = taskService.getTasks()
                .stream()
                .map(TaskResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{taskId}/reminders")
    @Operation(summary = "Get reminders for a task",
            description = "Calls reminder-service synchronously; falls back to an empty list " +
                    "if reminder-service is unavailable (see Resilience4j circuit breaker/retry).")
    public ResponseEntity<List<ReminderResponse>> getRemindersForTask(@PathVariable("taskId") final UUID taskId) {
        log.info("Get reminders for task {} request received", taskId);
        return ResponseEntity.ok(taskService.getRemindersForTask(taskId));
    }

    @PostMapping
    @Operation(summary = "Create a task",
            description = "Publishes a TaskCreated event that reminder-service consumes asynchronously.")
    public ResponseEntity<Void> createTask(
            @RequestBody @Valid final CreateTaskRequest createTaskRequest) {
        log.info("Create task request received");
        try {
            taskService.createTask(createTaskRequest);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (TaskValidationException e) {
            log.warn("Create task validation failed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

}
