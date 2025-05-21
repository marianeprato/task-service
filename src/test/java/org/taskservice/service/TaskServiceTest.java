package org.taskservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.taskservice.dto.CreateTaskRequest;
import org.taskservice.dto.ReminderRequest;
import org.taskservice.exception.TaskValidationException;
import org.taskservice.model.Task;
import org.taskservice.repository.TaskRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TaskService taskService;

    private Task sampleTask;
    private CreateTaskRequest createTaskRequest;

    @BeforeEach
    void setUp() {
        sampleTask = new Task(
                UUID.randomUUID(),
                "Test Task",
                "This is a sample task",
                LocalDate.now(),
                LocalDate.now().plusDays(5)
        );
        createTaskRequest = new CreateTaskRequest(
                "Test Task",
                "Task description",
                LocalDate.now().plusDays(5)
        );
    }

    @Test
    void shouldGetAllTasks() {
        when(taskRepository.findAll()).thenReturn(List.of(sampleTask));

        List<Task> tasks = taskService.getTasks();

        assertFalse(tasks.isEmpty());
        assertEquals(1, tasks.size());
        assertEquals("Test Task", tasks.get(0).taskTitle());
        verify(taskRepository, times(1)).findAll();
    }

    @Test
    void shouldGetTaskByIdWhenPresent() {
        when(taskRepository.findById(sampleTask.taskId())).thenReturn(sampleTask);

        Optional<Task> result = taskService.getTask(sampleTask.taskId());

        assertTrue(result.isPresent());
        assertEquals("Test Task", result.get().taskTitle());
        verify(taskRepository, times(1)).findById(sampleTask.taskId());
    }

    @Test
    void shouldReturnEmptyOptionalWhenTaskNotFound() {
        when(taskRepository.findById(sampleTask.taskId())).thenReturn(null);

        Optional<Task> result = taskService.getTask(sampleTask.taskId());

        assertFalse(result.isPresent());
        verify(taskRepository, times(1)).findById(sampleTask.taskId());
    }

    @Test
    void shouldCreateTaskAndTriggerReminder() {
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);

        taskService.createTask(createTaskRequest);

        verify(taskRepository, times(1)).save(taskCaptor.capture());
        Task savedTask = taskCaptor.getValue();

        ArgumentCaptor<ReminderRequest> reminderCaptor =
                ArgumentCaptor.forClass(ReminderRequest.class);

        verify(restTemplate, times(1))
                .postForEntity(
                        eq("http://localhost:8081/reminders"),
                        reminderCaptor.capture(),
                        eq(Void.class)
                );

        ReminderRequest sent = reminderCaptor.getValue();
        assertEquals(savedTask.taskId(), sent.getTaskId());
        assertTrue(sent.getMessage().startsWith("Reminder for task: " + createTaskRequest.taskTitle()));
    }


    @Test
    void shouldThrowExceptionForInvalidTaskTitle() {
        CreateTaskRequest invalidRequest =
                new CreateTaskRequest("", "Valid description", LocalDate.now().plusDays(1));

        TaskValidationException ex = assertThrows(
                TaskValidationException.class,
                () -> taskService.createTask(invalidRequest)
        );
        assertEquals("Task title cannot be empty", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionForPastDueDate() {
        CreateTaskRequest invalidRequest =
                new CreateTaskRequest("Valid title", "Valid description", LocalDate.now().minusDays(1));

        TaskValidationException ex = assertThrows(
                TaskValidationException.class,
                () -> taskService.createTask(invalidRequest)
        );
        assertEquals("Due date cannot be in the past", ex.getMessage());
    }

    @Test
    void shouldTriggerReminderDirectly() {
        taskService.triggerReminder(sampleTask);

        ArgumentCaptor<ReminderRequest> captor = ArgumentCaptor.forClass(ReminderRequest.class);

        verify(restTemplate, times(1))
                .postForEntity(
                        eq("http://localhost:8081/reminders"),
                        captor.capture(),
                        eq(Void.class)
                );

        ReminderRequest sent = captor.getValue();
        assertEquals(sampleTask.taskId(), sent.getTaskId());
        assertTrue(sent.getMessage().contains("Reminder for task: " + sampleTask.taskTitle()));
    }
}
