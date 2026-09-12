package org.taskservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.taskservice.client.ReminderClient;
import org.taskservice.dto.CreateTaskRequest;
import org.taskservice.dto.ReminderResponse;
import org.taskservice.exception.TaskValidationException;
import org.taskservice.model.Task;
import org.taskservice.model.TaskPriority;
import org.taskservice.outbox.OutboxEventFactory;
import org.taskservice.outbox.OutboxEventRepository;
import org.taskservice.repository.TaskRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ReminderClient reminderClient;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventFactory outboxEventFactory;

    public TaskService(
            TaskRepository taskRepository,
            ReminderClient reminderClient,
            OutboxEventRepository outboxEventRepository,
            OutboxEventFactory outboxEventFactory
    ) {
        this.taskRepository = taskRepository;
        this.reminderClient = reminderClient;
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventFactory = outboxEventFactory;
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

    /**
     * Saves the task and writes its TaskCreated outbox row in the same DB
     * transaction, so a crash right after this method returns can never
     * leave one written without the other -- either both are durable, or
     * neither is. Publishing to Kafka happens later, out-of-band, via
     * {@link org.taskservice.outbox.OutboxRelay}.
     */
    @Transactional
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
        outboxEventRepository.save(outboxEventFactory.forTaskCreated(newTask));
    }

}
