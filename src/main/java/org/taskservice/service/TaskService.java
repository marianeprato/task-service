package org.taskservice.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.taskservice.dto.CreateTaskRequest;
import org.taskservice.dto.ReminderRequest;
import org.taskservice.exception.TaskValidationException;
import org.taskservice.model.Task;
import org.taskservice.model.TaskPriority;
import org.taskservice.repository.TaskRepository;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final RestTemplate restTemplate;
    private final String reminderServiceBaseUrl;

    public TaskService(
            TaskRepository taskRepository,
            RestTemplate restTemplate,
            @Value("${reminder.service.base-url}") String reminderServiceBaseUrl) {
        this.taskRepository = taskRepository;
        this.restTemplate = restTemplate;
        this.reminderServiceBaseUrl = reminderServiceBaseUrl;
    }

    public List<Task> getTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> getTask(final UUID taskId) {
        return Optional.ofNullable(taskRepository.findById(taskId));
    }

    public void createTask(final CreateTaskRequest request) {
        if (request.taskTitle() == null || request.taskTitle().isBlank()) {
            throw new TaskValidationException("Task title cannot be empty");
        }
        if (request.taskDueDate().isBefore(LocalDate.now())) {
            throw new TaskValidationException("Due date cannot be in the past");
        }

        final TaskPriority priority = request.priority() != null ? request.priority() : TaskPriority.MEDIUM;

        final Task newTask = Task.builder()
                .taskId(UUID.randomUUID())
                .taskTitle(request.taskTitle())
                .taskDescription(request.taskDescription())
                .taskCreationDate(LocalDate.now())
                .taskDueDate(request.taskDueDate())
                .priority(priority)
                .build();

        taskRepository.save(newTask);
        triggerReminder(newTask);
    }

    @Async
    public void triggerReminder(Task task) {
        ReminderRequest reminderRequest = ReminderRequest.builder()
                .taskId(task.taskId())
                .message("Reminder for task: " + task.taskTitle())
                .build();
        String reminderEndpoint = reminderServiceBaseUrl + "/reminders";
        restTemplate.postForEntity(reminderEndpoint, reminderRequest, Void.class);
    }
}
