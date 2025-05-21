package org.taskservice.dto;

import java.util.UUID;

public class ReminderRequest {
    private UUID taskId;
    private String message;

    public ReminderRequest(UUID taskId, String message) {
        this.taskId = taskId;
        this.message = message;
    }

    public UUID getTaskId() { return taskId; }
    public String getMessage() { return message; }
}
