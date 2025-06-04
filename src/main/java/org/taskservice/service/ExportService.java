package org.taskservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.taskservice.client.ReminderClient;
import org.taskservice.dto.ReminderResponse;
import org.taskservice.model.Task;
import org.taskservice.repository.TaskRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private static final int COL_TASK_ID = 0;
    private static final int COL_TITLE = 1;
    private static final int COL_DESCRIPTION = 2;
    private static final int COL_CREATED = 3;
    private static final int COL_DUE = 4;
    private static final int COL_REMINDER_ID = 5;
    private static final int COL_REMINDER_MSG = 6;

    private final TaskRepository taskRepository;
    private final ReminderClient reminderClient;

    public byte[] generateExport() {
        List<Task> tasks = taskRepository.findAll();
        log.info("Generating export for {} tasks", tasks.size());

        try (
                XSSFWorkbook wb = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            Sheet sheet = wb.createSheet("Tasks & Reminders");
            createHeader(sheet);

            int rowIdx = 1;
            for (Task task : tasks) {
                List<ReminderResponse> reminders = reminderClient.getRemindersForTask(task.taskId());

                if (reminders.isEmpty()) {
                    Row row = sheet.createRow(rowIdx++);
                    fillTaskRow(row, task);
                } else {
                    for (ReminderResponse reminder : reminders) {
                        Row row = sheet.createRow(rowIdx++);
                        fillTaskRow(row, task);
                        row.createCell(COL_REMINDER_ID).setCellValue(reminder.reminderId());
                        row.createCell(COL_REMINDER_MSG).setCellValue(reminder.message());
                    }
                }
            }

            writeWorkbook(wb, out);

            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate Excel file", e);
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    void writeWorkbook(XSSFWorkbook wb, ByteArrayOutputStream out) throws IOException {
        wb.write(out);
    }

    private void createHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(COL_TASK_ID).setCellValue("Task ID");
        header.createCell(COL_TITLE).setCellValue("Title");
        header.createCell(COL_DESCRIPTION).setCellValue("Description");
        header.createCell(COL_CREATED).setCellValue("Created");
        header.createCell(COL_DUE).setCellValue("Due");
        header.createCell(COL_REMINDER_ID).setCellValue("Reminder ID");
        header.createCell(COL_REMINDER_MSG).setCellValue("Reminder Message");
    }

    private void fillTaskRow(Row row, Task task) {
        row.createCell(COL_TASK_ID).setCellValue(task.taskId().toString());
        row.createCell(COL_TITLE).setCellValue(task.taskTitle());
        row.createCell(COL_DESCRIPTION).setCellValue(task.taskDescription());
        row.createCell(COL_CREATED).setCellValue(task.taskCreationDate().toString());
        row.createCell(COL_DUE).setCellValue(task.taskDueDate().toString());
    }
}
