package org.taskservice.dto;

import java.util.UUID;

public record ReminderResponse(
        long reminderId,
        UUID taskId,
        String message
) {}
