package org.taskservice.service.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.springframework.http.MediaType;
import org.taskservice.client.ReminderClient;
import org.taskservice.model.Task;

public class CsvExportStrategy implements ExportStrategy {

    private final ReminderClient reminderClient;

    public CsvExportStrategy(ReminderClient reminderClient) {
        this.reminderClient = reminderClient;
    }

    @Override
    public String getFileExtension() {
        return ".csv";
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.parseMediaType("text/csv");
    }

    @Override
    public byte[] export(List<Task> tasks) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintWriter writer = new PrintWriter(out)) {
            writer.println("Task ID,Title,Description,Created,Due,Priority,Reminder Message");
            for (Task task : tasks) {
                writer.printf("\"%s\",\"%s\",\"...\"\n", task.taskId(), task.taskTitle());
            }
            writer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV file", e);
        }
    }
}
