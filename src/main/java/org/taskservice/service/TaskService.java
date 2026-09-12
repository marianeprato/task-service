package org.taskservice.service;

import org.springframework.stereotype.Service;
import org.taskservice.client.ReminderClient;
import org.taskservice.dto.CreateTaskRequest;
import org.taskservice.dto.ReminderResponse;
import org.taskservice.event.TaskEventProducer;
import org.taskservice.exception.TaskValidationException;
import org.taskservice.model.Task;
import org.taskservice.model.TaskPriority;
import org.taskservice.repository.TaskRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ReminderClient reminderClient;
    private final TaskEventProducer taskEventProducer;

    public TaskService(
            TaskRepository taskRepository,
            ReminderClient reminderClient,
            TaskEventProducer taskEventProducer
    ) {
        this.taskRepository = taskRepository;
        this.reminderClient = reminderClient;
        this.taskEventProducer = taskEventProducer;
    }

    public List<Task> getTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> getTask(final UUID taskId) {
        return Optional.ofNullable(taskRepository.findById(taskId));
    }

    public List<ReminderResponse> getRemindersForTask(final UUID taskId) {
        return reminderClient.getRemindersForTask(taskId);
    }

    public void createTask(final CreateTaskRequest request) {
        if (request.taskTitle() == null || request.taskTitle().isBlank()) {
            throw new TaskValidationException("Task title cannot be empty");
        }
        if (request.taskDueDate().isBefore(LocalDate.now())) {
            throw new TaskValidationException("Due date cannot be in the past");
        }

        final TaskPriority priority = request.priority() != null
                ? request.priority()
                : TaskPriority.MEDIUM;

        final Task newTask = new Task(
                UUID.randomUUID(),
                request.taskTitle(),
                request.taskDescription(),
                LocalDate.now(),
                request.taskDueDate(),
                priority
        );

        taskRepository.save(newTask);
        taskEventProducer.publishTaskCreated(newTask);
    }

}
