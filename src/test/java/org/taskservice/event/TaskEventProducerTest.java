package org.taskservice.event;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.taskservice.model.Task;
import org.taskservice.model.TaskPriority;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"task-created"})
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
class TaskEventProducerTest {

    @Autowired
    private TaskEventProducer taskEventProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Value("${app.kafka.topic.task-created}")
    private String topic;

    private Consumer<String, TaskCreatedEvent> consumer;

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void publishesTaskCreatedEventToKafka() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("task-event-test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "org.taskservice.event");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TaskCreatedEvent.class.getName());
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerProps);
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, topic);

        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Write report", "Quarterly report",
                LocalDate.now(), LocalDate.now().plusDays(2), TaskPriority.HIGH);

        taskEventProducer.publishTaskCreated(task);

        ConsumerRecords<String, TaskCreatedEvent> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertEquals(1, records.count());

        ConsumerRecord<String, TaskCreatedEvent> record = records.iterator().next();
        assertEquals(taskId.toString(), record.key());
        assertNotNull(record.value());
        assertEquals(taskId, record.value().taskId());
        assertEquals("Write report", record.value().taskTitle());
        assertEquals(TaskPriority.HIGH, record.value().priority());
    }
}
