package org.taskservice.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ReminderResponse(
        long reminderId,
        UUID taskId,
        String message
) {}