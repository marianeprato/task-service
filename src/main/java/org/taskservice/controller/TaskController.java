package org.taskservice.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.taskservice.dto.CreateTaskRequest;
import org.taskservice.dto.TaskResponse;
import org.taskservice.exception.TaskValidationException;
import org.taskservice.service.TaskService;

import java.util.logging.Logger;
import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private static final Logger LOGGER = Logger.getLogger(TaskController.class.getName());
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks() {
        LOGGER.info( "Get all tasks request received");
        final List<TaskResponse> list = taskService.getTasks()
                .stream()
                .map(TaskResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<Void> createTask(@RequestBody @Valid final CreateTaskRequest createTaskRequest) {
        LOGGER.info("Create task request received");
        try {
            taskService.createTask(createTaskRequest);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (TaskValidationException e) {
            LOGGER.warning("Create task validation failed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

}
