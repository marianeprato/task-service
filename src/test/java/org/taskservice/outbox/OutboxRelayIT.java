package org.taskservice.outbox;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.taskservice.dto.CreateTaskRequest;
import org.taskservice.model.TaskPriority;
import org.taskservice.service.TaskService;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end check of the outbox: a task is created through the real
 * TaskService (real Postgres via Testcontainers, real transaction), and the
 * real, scheduled OutboxRelay bean must pick up the resulting row and
 * publish it to an embedded Kafka broker, then mark it published.
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"task-created"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "outbox.relay.poll-interval-ms=300"
})
class OutboxRelayIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TaskService taskService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Value("${app.kafka.topic.task-created}")
    private String topic;

    private Consumer<String, String> consumer;

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void outboxRowWrittenOnCreateIsRelayedToKafkaAndMarkedPublished() {
        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps("outbox-relay-test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumer = new KafkaConsumer<>(consumerProps);
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, topic);

        String uniqueTitle = "Outbox relay test " + UUID.randomUUID();
        CreateTaskRequest request = new CreateTaskRequest(
                uniqueTitle, "Verifying the outbox relay", LocalDate.now().plusDays(1), TaskPriority.MEDIUM);

        taskService.createTask(request);

        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        List<ConsumerRecord<String, String>> matching = StreamSupport.stream(records.spliterator(), false)
                .filter(r -> r.value().contains(uniqueTitle))
                .toList();
        assertThat(matching).hasSize(1);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<OutboxEventEntity> stillUnpublished = outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
            assertThat(stillUnpublished).noneMatch(e -> e.getPayload().contains(uniqueTitle));
        });
    }
}
