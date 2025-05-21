package org.taskservice.repository;

import org.springframework.stereotype.Repository;
import org.taskservice.model.Task;

import java.util.*;

@Repository
public class TaskRepository {

    private final Map<UUID, Task> tasks = new HashMap<>();

    public void  save(final Task task) { tasks.put(task.taskId(), task); }

    public Task findById(final  UUID taskId) { return tasks.get(taskId); }

    public List<Task> findAll() { return new ArrayList<>(tasks.values()); }
}
