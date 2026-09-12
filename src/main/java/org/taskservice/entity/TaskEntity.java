package org.taskservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.taskservice.model.Task;
import org.taskservice.model.TaskPriority;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskEntity {

    @Id
    private UUID taskId;

    private String taskTitle;

    private String taskDescription;

    private LocalDate taskCreationDate;

    private LocalDate taskDueDate;

    @Enumerated(EnumType.STRING)
    private TaskPriority priority;

    public static TaskEntity from(Task task) {
        return TaskEntity.builder()
                .taskId(task.taskId())
                .taskTitle(task.taskTitle())
                .taskDescription(task.taskDescription())
                .taskCreationDate(task.taskCreationDate())
                .taskDueDate(task.taskDueDate())
                .priority(task.priority())
                .build();
    }

    public Task toDomain() {
        return new Task(taskId, taskTitle, taskDescription, taskCreationDate, taskDueDate, priority);
    }
}
