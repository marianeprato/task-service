package org.taskservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.taskservice.model.Task;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskRepositoryTest {

    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        taskRepository = new TaskRepository();
    }

    @Test
    void shouldSaveAndFindById() {
        UUID id = UUID.randomUUID();
        Task task = new Task(
                id,
                "Sample Task",
                "Sample description",
                LocalDate.now(),
                LocalDate.now().plusDays(3)
        );

        taskRepository.save(task);
        Task found = taskRepository.findById(id);

        assertEquals(task, found);
    }

    @Test
    void shouldReturnNullWhenTaskNotPresent() {
        UUID nonexistentId = UUID.randomUUID();

        Task found = taskRepository.findById(nonexistentId);

        assertNull(found);
    }

    @Test
    void shouldReturnAllSavedTasks() {
        Task first = new Task(
                UUID.randomUUID(),
                "First Task",
                "First description",
                LocalDate.now(),
                LocalDate.now().plusDays(1)
        );
        Task second = new Task(
                UUID.randomUUID(),
                "Second Task",
                "Second description",
                LocalDate.now(),
                LocalDate.now().plusDays(2)
        );

        taskRepository.save(first);
        taskRepository.save(second);

        List<Task> all = taskRepository.findAll();

        assertTrue(all.contains(first));
        assertTrue(all.contains(second));
        assertEquals(2, all.size());
    }

    @Test
    void shouldOverwriteTaskWhenSavingWithSameId() {
        UUID id = UUID.randomUUID();
        Task original = new Task(
                id,
                "Original Task",
                "Original description",
                LocalDate.now(),
                LocalDate.now().plusDays(1)
        );
        taskRepository.save(original);

        Task updated = new Task(
                id,
                "Updated Task",
                "Updated description",
                LocalDate.now(),
                LocalDate.now().plusDays(5)
        );
        taskRepository.save(updated);

        Task found = taskRepository.findById(id);

        assertEquals("Updated Task", found.taskTitle());
        assertEquals("Updated description", found.taskDescription());
    }
}
