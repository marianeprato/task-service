package org.taskservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;
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
