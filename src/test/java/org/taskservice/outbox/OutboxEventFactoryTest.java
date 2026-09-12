package org.taskservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.taskservice.correlation.CorrelationIdContext;
import org.taskservice.model.Task;
import org.taskservice.model.TaskPriority;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventFactoryTest {

    private final OutboxEventFactory factory =
            new OutboxEventFactory(new ObjectMapper().registerModule(new JavaTimeModule()));

    @BeforeEach
    void setUp() {
        CorrelationIdContext.clear();
    }

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    @Test
    void buildsAnOutboxRowCarryingTheTaskAsJsonPayload() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Write report", "Quarterly report",
                LocalDate.now(), LocalDate.now().plusDays(2), TaskPriority.HIGH);

        OutboxEventEntity event = factory.forTaskCreated(task);

        assertThat(event.getId()).isNotNull();
        assertThat(event.getAggregateType()).isEqualTo("Task");
        assertThat(event.getAggregateId()).isEqualTo(taskId);
        assertThat(event.getEventType()).isEqualTo("TaskCreated");
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getPayload())
                .contains(taskId.toString())
                .contains("Write report")
                .contains("HIGH");
    }

    @Test
    void capturesTheCurrentCorrelationIdOnTheRequestThread() {
        CorrelationIdContext.set("test-correlation-id");
        Task task = new Task(UUID.randomUUID(), "Task", "Description",
                LocalDate.now(), LocalDate.now().plusDays(1), TaskPriority.MEDIUM);

        OutboxEventEntity event = factory.forTaskCreated(task);

        assertThat(event.getCorrelationId()).isEqualTo("test-correlation-id");
    }

    @Test
    void toleratesAMissingCorrelationId() {
        Task task = new Task(UUID.randomUUID(), "Task", "Description",
                LocalDate.now(), LocalDate.now().plusDays(1), TaskPriority.MEDIUM);

        OutboxEventEntity event = factory.forTaskCreated(task);

        assertThat(event.getCorrelationId()).isNull();
    }
}
