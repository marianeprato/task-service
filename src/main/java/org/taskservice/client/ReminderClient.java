package org.taskservice.client;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.taskservice.dto.ReminderResponse;

@Component
public class ReminderClient {

    private final RestTemplate restTemplate;
    private final String reminderServiceBaseUrl;

    public ReminderClient(
            RestTemplate restTemplate, @Value("${reminder.service.base-url}") String reminderServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.reminderServiceBaseUrl = reminderServiceBaseUrl;
    }

    public List<ReminderResponse> getRemindersForTask(UUID taskId) {
        String url = reminderServiceBaseUrl + "/reminders/{taskId}";
        ReminderResponse[] response = restTemplate.getForObject(url, ReminderResponse[].class, taskId);
        return response != null ? Arrays.asList(response) : List.of();
    }
}
