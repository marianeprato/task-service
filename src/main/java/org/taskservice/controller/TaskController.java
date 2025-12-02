package org.taskservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.taskservice.dto.CreateTaskRequest;
import org.taskservice.dto.TaskResponse;
import org.taskservice.exception.TaskValidationException;
import org.taskservice.service.TaskService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks() {
        log.info("Get all tasks request received");
        final List<TaskResponse> list =
                taskService.getTasks().stream().map(TaskResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<Void> createTask(@RequestBody @Valid final CreateTaskRequest createTaskRequest) {
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
