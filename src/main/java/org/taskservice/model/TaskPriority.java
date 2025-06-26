package org.taskservice.model;

public enum TaskPriority {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    private final String value;

    TaskPriority(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}