package org.taskservice.service.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.taskservice.client.ReminderClient;
import org.taskservice.dto.ReminderResponse;
import org.taskservice.model.Task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ExcelExportStrategy implements ExportStrategy {

    private static final int COL_TASK_ID = 0;
    private static final int COL_TITLE = 1;
    private static final int COL_DESCRIPTION = 2;
    private static final int COL_CREATED = 3;
    private static final int COL_DUE = 4;
    private static final int COL_REMINDER_ID = 5;
    private static final int COL_REMINDER_MSG = 6;
    static final String SHEET_NAME = "Tasks & Reminders";
    private static final int COL_PRIORITY = 7;

    private final ReminderClient reminderClient;

    @Override
    public byte[] export(List<Task> tasks) {
        log.info("Generating export for {} tasks", tasks.size());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            createHeader(sheet);
            populateRows(sheet, tasks, 1);
            writeWorkbook(workbook, out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate Excel file", e);
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    @Override
    public String getFileExtension() {
        return ".xlsx";
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    void writeWorkbook(XSSFWorkbook workbook, ByteArrayOutputStream out) throws IOException {
        workbook.write(out);
    }

    private int populateRows(Sheet sheet, List<Task> tasks, int startRowIdx) {
        final int[] rowIndex = {startRowIdx};

        tasks.forEach(task -> {
            List<ReminderResponse> reminders = reminderClient.getRemindersForTask(task.taskId());

            if (reminders.isEmpty()) {
                rowIndex[0] = writeTaskOnlyRow(sheet, task, rowIndex[0]);
            } else {
                reminders.forEach(
                        reminder -> rowIndex[0] = writeTaskWithReminderRow(sheet, task, reminder, rowIndex[0]));
            }
        });

        return rowIndex[0];
    }

    private int writeTaskOnlyRow(Sheet sheet, Task task, int rowIndex) {
        Row row = sheet.createRow(rowIndex);
        fillTaskRow(row, task);
        return rowIndex + 1;
    }

    private int writeTaskWithReminderRow(Sheet sheet, Task task, ReminderResponse reminder, int rowIndex) {
        Row row = sheet.createRow(rowIndex);
        fillTaskRow(row, task);
        row.createCell(COL_REMINDER_ID).setCellValue(reminder.reminderId());
        row.createCell(COL_REMINDER_MSG).setCellValue(reminder.message());
        return rowIndex + 1;
    }

    private static void createHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(COL_TASK_ID).setCellValue("Task ID");
        header.createCell(COL_TITLE).setCellValue("Title");
        header.createCell(COL_DESCRIPTION).setCellValue("Description");
        header.createCell(COL_CREATED).setCellValue("Created");
        header.createCell(COL_DUE).setCellValue("Due");
        header.createCell(COL_REMINDER_ID).setCellValue("Reminder ID");
        header.createCell(COL_REMINDER_MSG).setCellValue("Reminder Message");
        header.createCell(COL_PRIORITY).setCellValue("Priority");
    }

    private static void fillTaskRow(Row row, Task task) {
        row.createCell(COL_TASK_ID).setCellValue(task.taskId().toString());
        row.createCell(COL_TITLE).setCellValue(task.taskTitle());
        row.createCell(COL_DESCRIPTION).setCellValue(task.taskDescription());
        row.createCell(COL_CREATED).setCellValue(task.taskCreationDate().toString());
        row.createCell(COL_DUE).setCellValue(task.taskDueDate().toString());
        row.createCell(COL_PRIORITY).setCellValue(task.priority().getValue());
    }
}
