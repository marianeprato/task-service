package org.taskservice.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReminderRequest {
    private UUID taskId;
    private String message;

    public ReminderRequest(UUID taskId, String message) {
        this.taskId = taskId;
        this.message = message;
    }
}
