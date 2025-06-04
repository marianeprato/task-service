package org.taskservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.taskservice.dto.ReminderResponse;

import java.util.List;
import java.util.UUID;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class ReminderClient {

    private final RestTemplate restTemplate;
    private static final String REMINDER_SERVICE_URL = "http://localhost:8081/reminders/{taskId}";

    public List<ReminderResponse> getRemindersForTask(UUID taskId) {
        ReminderResponse[] response = restTemplate.getForObject(
                REMINDER_SERVICE_URL,
                ReminderResponse[].class,
                taskId
        );
        return response != null ? Arrays.asList(response) : List.of();
    }
}
