package org.taskservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.taskservice.model.Task;

import java.time.Instant;

@Component
public class TaskEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventProducer.class);

    private final KafkaTemplate<String, TaskCreatedEvent> kafkaTemplate;
    private final String taskCreatedTopic;

    public TaskEventProducer(KafkaTemplate<String, TaskCreatedEvent> kafkaTemplate,
                              @Value("${app.kafka.topic.task-created}") String taskCreatedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.taskCreatedTopic = taskCreatedTopic;
    }

    public void publishTaskCreated(Task task) {
        TaskCreatedEvent event = new TaskCreatedEvent(
                task.taskId(),
                task.taskTitle(),
                task.taskDescription(),
                task.taskDueDate(),
                task.priority(),
                Instant.now()
        );

        kafkaTemplate.send(taskCreatedTopic, task.taskId().toString(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.warn("Failed to publish TaskCreated event for taskId={}: {}",
                                task.taskId(), exception.toString());
                    } else {
                        log.info("Published TaskCreated event for taskId={} to topic {} (offset={})",
                                task.taskId(), taskCreatedTopic, result.getRecordMetadata().offset());
                    }
                });
    }
}
