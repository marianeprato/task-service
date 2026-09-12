package org.taskservice.client;

import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.taskservice.dto.ReminderResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the resilience behavior (retry, circuit breaker, timeout, fallback)
 * of {@link ReminderClient} against real sockets: a dead port simulates
 * reminder-service being down, and a deliberately slow HTTP handler simulates
 * reminder-service being unresponsive.
 */
class ReminderClientResilienceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private RestTemplate restTemplateWithTimeouts(long connectMs, long readMs) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(connectMs))
                .withReadTimeout(Duration.ofMillis(readMs));
        return new RestTemplate(ClientHttpRequestFactories.get(settings));
    }

    private ReminderClient buildClient(RestTemplate restTemplate, String baseUrl) {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(20), 2))
                .retryExceptions(ResourceAccessException.class, HttpServerErrorException.class)
                .build();
        RetryRegistry retryRegistry = RetryRegistry.of(Map.of(ReminderClient.BACKEND, retryConfig));

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(500))
                .build();
        CircuitBreakerRegistry circuitBreakerRegistry =
                CircuitBreakerRegistry.of(Map.of(ReminderClient.BACKEND, circuitBreakerConfig));

        return new ReminderClient(restTemplate, baseUrl, circuitBreakerRegistry, retryRegistry);
    }

    @Test
    void fallsBackToEmptyListWhenReminderServiceIsDown() throws IOException {
        int deadPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            deadPort = socket.getLocalPort();
        }
        String baseUrl = "http://localhost:" + deadPort;

        ReminderClient client = buildClient(restTemplateWithTimeouts(200, 500), baseUrl);

        List<ReminderResponse> result = client.getRemindersForTask(UUID.randomUUID());

        assertTrue(result.isEmpty(), "Should fall back to an empty list when reminder-service is unreachable");
    }

    @Test
    void retriesAndFallsBackWhenReminderServiceIsSlow() throws IOException {
        AtomicInteger callCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/reminders", exchange -> {
            callCount.incrementAndGet();
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] body = "[]".getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        ReminderClient client = buildClient(restTemplateWithTimeouts(200, 100), baseUrl);

        List<ReminderResponse> result = client.getRemindersForTask(UUID.randomUUID());

        assertTrue(result.isEmpty(), "Should fall back to an empty list on repeated timeouts");
        assertEquals(3, callCount.get(), "Should retry up to the configured max attempts before falling back");
    }

    @Test
    void returnsRemindersWhenReminderServiceRespondsNormally() throws IOException {
        UUID taskId = UUID.randomUUID();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/reminders", exchange -> {
            String body = "[{\"reminderId\":1,\"taskId\":\"" + taskId + "\",\"message\":\"Reminder\"}]";
            byte[] bytes = body.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        ReminderClient client = buildClient(restTemplateWithTimeouts(500, 500), baseUrl);

        List<ReminderResponse> result = client.getRemindersForTask(taskId);

        assertEquals(1, result.size());
        assertEquals(taskId, result.get(0).taskId());
    }
}
