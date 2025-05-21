package org.taskservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.taskservice.dto.CreateTaskRequest;
import org.taskservice.dto.TaskResponse;
import org.taskservice.exception.TaskValidationException;
import org.taskservice.service.TaskService;
import org.taskservice.model.Task;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private Task sampleTask;
    private CreateTaskRequest validRequest;

    @BeforeEach
    void init() {
        sampleTask = new Task(
                UUID.randomUUID(),
                "Sample Task",
                "Sample description",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 10)
        );
        validRequest = new CreateTaskRequest(
                "Title",
                "Desc",
                LocalDate.of(2025, 2, 15)
        );
    }

    @Test
    void getTasks_emptyList() {
        when(taskService.getTasks()).thenReturn(List.of());

        ResponseEntity<List<TaskResponse>> response = taskController.getTasks();

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty(), "Body should be empty when no tasks");
        verify(taskService, times(1)).getTasks();
    }

    @Test
    void getTasks_mappingFieldsCorrectly() {
        when(taskService.getTasks()).thenReturn(List.of(sampleTask));

        ResponseEntity<List<TaskResponse>> response = taskController.getTasks();
        List<TaskResponse> body = response.getBody();

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(body);
        assertEquals(1, body.size());
        TaskResponse tr = body.get(0);
        assertEquals(sampleTask.taskId(), tr.taskId(), "ID should match");
        assertEquals(sampleTask.taskTitle(), tr.taskTitle(), "Title should match");
        assertEquals(sampleTask.taskDescription(), tr.taskDescription(), "Description should match");
        assertEquals(sampleTask.taskCreationDate(), tr.taskCreationDate(), "Creation date should match");
        assertEquals(sampleTask.taskDueDate(), tr.taskDueDate(), "Due date should match");
    }

    @Test
    void createTask_noBodyAndServiceCalled() {
        ResponseEntity<Void> response = taskController.createTask(validRequest);

        assertEquals(201, response.getStatusCodeValue());
        assertNull(response.getBody(), "Response body should be null for CREATED");
        verify(taskService, times(1)).createTask(validRequest);
    }

    @Test
    void createTask_validationException_logsAndReturnsBadRequest() {
        doThrow(new TaskValidationException("Bad data")).when(taskService).createTask(any());

        ResponseEntity<Void> response = taskController.createTask(
                new CreateTaskRequest("","",LocalDate.now())
        );

        assertEquals(400, response.getStatusCodeValue());
        assertNull(response.getBody(), "Response body should be null for BAD_REQUEST");
        verify(taskService, times(1)).createTask(any());
    }
}