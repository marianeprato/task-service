package org.taskservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.taskservice.client.ReminderClient;
import org.taskservice.dto.CreateTaskRequest;
import org.taskservice.dto.ReminderResponse;
import org.taskservice.event.TaskEventProducer;
import org.taskservice.exception.TaskValidationException;
import org.taskservice.model.Task;
import org.taskservice.model.TaskPriority;
import org.taskservice.repository.TaskRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository mockTaskRepository;

    @Mock
    private ReminderClient mockReminderClient;

    @Mock
    private TaskEventProducer mockTaskEventProducer;

    @InjectMocks
    private TaskService taskService;

    private Task sampleTask;
    private CreateTaskRequest validCreateRequest;

    @BeforeEach
    void setUp() {
        final UUID taskId = UUID.randomUUID();
        final String title = "Test Task";
        final String description = "This is a sample task";
        final LocalDate creationDate = LocalDate.now();
        final LocalDate dueDate = creationDate.plusDays(5);

        sampleTask = new Task(taskId, title, description, creationDate, dueDate, TaskPriority.HIGH);
        validCreateRequest = new CreateTaskRequest(title, "Task description", dueDate, TaskPriority.HIGH);
    }

    @Test
    void shouldGetAllTasks() {
        when(mockTaskRepository.findAll()).thenReturn(Collections.singletonList(sampleTask));

        final List<Task> tasks = taskService.getTasks();

        assertEquals(1, tasks.size());
        assertEquals("Test Task", tasks.get(0).taskTitle());
        verify(mockTaskRepository, times(1)).findAll();
    }

    @Test
    void shouldGetTaskByIdWhenPresent() {
        when(mockTaskRepository.findById(sampleTask.taskId())).thenReturn(sampleTask);

        final Optional<Task> result = taskService.getTask(sampleTask.taskId());

        assertTrue(result.isPresent());
        assertEquals("Test Task", result.get().taskTitle());
        verify(mockTaskRepository, times(1)).findById(sampleTask.taskId());
    }

    @Test
    void shouldReturnEmptyOptionalWhenTaskNotFound() {
        when(mockTaskRepository.findById(sampleTask.taskId())).thenReturn(null);

        final Optional<Task> result = taskService.getTask(sampleTask.taskId());

        assertFalse(result.isPresent());
        verify(mockTaskRepository, times(1)).findById(sampleTask.taskId());
    }

    @Test
    void shouldPersistTaskAndPublishTaskCreatedEvent() {
        taskService.createTask(validCreateRequest);

        final ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(mockTaskRepository, times(1)).save(taskCaptor.capture());
        verify(mockTaskEventProducer, times(1)).publishTaskCreated(taskCaptor.getValue());

        final Task savedTask = taskCaptor.getValue();
        assertEquals(validCreateRequest.taskTitle(), savedTask.taskTitle());
        assertEquals(TaskPriority.HIGH, savedTask.priority());
    }

    @Test
    void shouldThrowExceptionForInvalidTaskTitle() {
        final CreateTaskRequest invalidRequest = new CreateTaskRequest("", "Valid description", LocalDate.now().plusDays(1), null);

        final TaskValidationException exception = assertThrows(
                TaskValidationException.class,
                () -> taskService.createTask(invalidRequest)
        );
        assertEquals("Task title cannot be empty", exception.getMessage());
        verify(mockTaskEventProducer, never()).publishTaskCreated(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldThrowExceptionForPastDueDate() {
        final CreateTaskRequest invalidRequest = new CreateTaskRequest("Valid title", "Valid description", LocalDate.now().minusDays(1), null);

        final TaskValidationException exception = assertThrows(
                TaskValidationException.class,
                () -> taskService.createTask(invalidRequest)
        );
        assertEquals("Due date cannot be in the past", exception.getMessage());
        verify(mockTaskEventProducer, never()).publishTaskCreated(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldDelegateReminderLookupToReminderClient() {
        final List<ReminderResponse> expected = List.of(
                new ReminderResponse(1L, sampleTask.taskId(), "Reminder for task: " + sampleTask.taskTitle())
        );
        when(mockReminderClient.getRemindersForTask(sampleTask.taskId())).thenReturn(expected);

        final List<ReminderResponse> result = taskService.getRemindersForTask(sampleTask.taskId());

        assertEquals(expected, result);
        verify(mockReminderClient, times(1)).getRemindersForTask(sampleTask.taskId());
    }

    @Test
    void shouldCreateTaskWithProvidedPriority() {
        CreateTaskRequest request = new CreateTaskRequest(
                "High Priority Task",
                "Important work",
                LocalDate.now().plusDays(3),
                TaskPriority.HIGH
        );

        taskService.createTask(request);

        final ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(mockTaskRepository).save(taskCaptor.capture());

        final Task savedTask = taskCaptor.getValue();
        assertEquals(TaskPriority.HIGH, savedTask.priority());
    }
}
