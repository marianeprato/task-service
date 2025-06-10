package org.taskservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.taskservice.dto.ReminderResponse;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReminderClient {

    private final RestTemplate restTemplate;

    @Value("${reminder.service.base-url}")
    private final String reminderServiceBaseUrl;

    public List<ReminderResponse> getRemindersForTask(UUID taskId) {
        final String url = reminderServiceBaseUrl + "/reminders/{taskId}";

        ReminderResponse[] response = restTemplate.getForObject(
                url,
                ReminderResponse[].class,
                taskId
        );
        return response != null ? Arrays.asList(response) : List.of();
    }
}
