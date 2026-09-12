package org.taskservice.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.taskservice.dto.ReminderResponse;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class ReminderClient {

    private static final Logger log = LoggerFactory.getLogger(ReminderClient.class);
    public static final String BACKEND = "reminderService";

    private final RestTemplate restTemplate;
    private final String reminderServiceBaseUrl;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public ReminderClient(RestTemplate restTemplate,
                           @Value("${reminder.service.base-url}") String reminderServiceBaseUrl,
                           CircuitBreakerRegistry circuitBreakerRegistry,
                           RetryRegistry retryRegistry) {
        this.restTemplate = restTemplate;
        this.reminderServiceBaseUrl = reminderServiceBaseUrl;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(BACKEND);
        this.retry = retryRegistry.retry(BACKEND);
    }

    public List<ReminderResponse> getRemindersForTask(UUID taskId) {
        Supplier<List<ReminderResponse>> call = () -> fetchReminders(taskId);
        Supplier<List<ReminderResponse>> resilientCall =
                Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, call));

        try {
            return resilientCall.get();
        } catch (Exception e) {
            return fallbackToEmptyList(taskId, e);
        }
    }

    private List<ReminderResponse> fetchReminders(UUID taskId) {
        String url = reminderServiceBaseUrl + "/reminders/{taskId}";
        ReminderResponse[] response = restTemplate.getForObject(
                url, ReminderResponse[].class, taskId);
        return response != null ? Arrays.asList(response) : List.of();
    }

    private List<ReminderResponse> fallbackToEmptyList(UUID taskId, Throwable throwable) {
        log.warn("reminder-service unavailable while fetching reminders for taskId={}, " +
                "returning empty list. Cause: {}", taskId, throwable.toString());
        return List.of();
    }
}
