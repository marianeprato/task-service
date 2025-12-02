package org.taskservice.dto;

import java.util.UUID;

import lombok.Builder;

@Builder
public record ReminderResponse(long reminderId, UUID taskId, String message) {}
