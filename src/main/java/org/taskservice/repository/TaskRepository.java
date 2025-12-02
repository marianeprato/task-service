package org.taskservice.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.taskservice.model.Task;

@Repository
public class TaskRepository {

    private final Map<UUID, Task> tasks = new HashMap<>();

    public void save(final Task task) {
        tasks.put(task.taskId(), task);
    }

    public Task findById(final UUID taskId) {
        return tasks.get(taskId);
    }

    public List<Task> findAll() {
        return new ArrayList<>(tasks.values());
    }
}
