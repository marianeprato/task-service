package org.taskservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.taskservice.entity.TaskEntity;

import java.util.UUID;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, UUID> {
}
