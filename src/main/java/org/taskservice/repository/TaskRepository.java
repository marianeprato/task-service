package org.taskservice.repository;

import org.springframework.stereotype.Repository;
import org.taskservice.entity.TaskEntity;
import org.taskservice.model.Task;

import java.util.List;
import java.util.UUID;

/**
 * Thin adapter over Spring Data JPA so the rest of the codebase (TaskService,
 * its tests) keeps working against the plain {@link Task} domain record
 * without knowing about {@link TaskEntity} or JPA.
 */
@Repository
public class TaskRepository {

    private final TaskJpaRepository jpaRepository;

    public TaskRepository(TaskJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public void save(final Task task) {
        jpaRepository.save(TaskEntity.from(task));
    }

    public Task findById(final UUID taskId) {
        return jpaRepository.findById(taskId)
                .map(TaskEntity::toDomain)
                .orElse(null);
    }

    public List<Task> findAll() {
        return jpaRepository.findAll().stream()
                .map(TaskEntity::toDomain)
                .toList();
    }
}
