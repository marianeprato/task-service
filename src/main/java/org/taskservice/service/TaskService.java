package org.taskservice.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
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

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final RestTemplate restTemplate;

    public TaskService(TaskRepository taskRepository, RestTemplate restTemplate) {
        this.taskRepository = taskRepository;
        this.restTemplate = restTemplate;
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

        Task newTask = new Task(
                UUID.randomUUID(),
                request.taskTitle(),
                request.taskDescription(),
                LocalDate.now(),
                request.taskDueDate()
        );

        taskRepository.save(newTask);


        triggerReminder(newTask);
    }

    @Async
    public void triggerReminder(Task task) {
        ReminderRequest reminderRequest = new ReminderRequest(task.taskId(), "Reminder for task: " + task.taskTitle());
        String reminderServiceUrl = "http://localhost:8081/reminders";
        restTemplate.postForEntity(reminderServiceUrl, reminderRequest, Void.class);
    }

}
