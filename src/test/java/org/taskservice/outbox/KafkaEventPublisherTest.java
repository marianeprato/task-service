package org.taskservice.outbox;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.taskservice.correlation.CorrelationIdContext;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsTheRecordWithACorrelationIdHeaderWhenPresent() throws Exception {
        SendResult<String, String> result = mock(SendResult.class);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(result));
        KafkaEventPublisher publisher = new KafkaEventPublisher(kafkaTemplate);

        publisher.publish("task-created", "key-1", "{\"taskId\":\"abc\"}", "corr-123");

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, String> sent = captor.getValue();

        assertThat(sent.topic()).isEqualTo("task-created");
        assertThat(sent.key()).isEqualTo("key-1");
        assertThat(sent.value()).isEqualTo("{\"taskId\":\"abc\"}");

        Header header = sent.headers().lastHeader(CorrelationIdContext.HEADER_NAME);
        assertThat(header).isNotNull();
        assertThat(new String(header.value(), StandardCharsets.UTF_8)).isEqualTo("corr-123");
    }

    @Test
    @SuppressWarnings("unchecked")
    void omitsTheHeaderWhenNoCorrelationIdIsGiven() throws Exception {
        SendResult<String, String> result = mock(SendResult.class);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(result));
        KafkaEventPublisher publisher = new KafkaEventPublisher(kafkaTemplate);

        publisher.publish("task-created", "key-1", "{}", null);

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());

        assertThat(captor.getValue().headers().lastHeader(CorrelationIdContext.HEADER_NAME)).isNull();
    }

    @Test
    void wrapsSendFailuresInKafkaPublishException() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker unreachable")));
        KafkaEventPublisher publisher = new KafkaEventPublisher(kafkaTemplate);

        assertThatThrownBy(() -> publisher.publish("task-created", "key-1", "{}", null))
                .isInstanceOf(KafkaEventPublisher.KafkaPublishException.class);
    }
}
