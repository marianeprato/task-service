package org.taskservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.taskservice.dto.ReminderResponse;
import org.taskservice.model.Task;
import org.taskservice.repository.TaskRepository;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/export")
public class ExportController {

    private static final Logger log = LoggerFactory.getLogger(ExportController.class);

    private final TaskRepository taskRepo;
    private final RestTemplate restTemplate;

    public ExportController(TaskRepository taskRepo, RestTemplate restTemplate) {
        this.taskRepo = taskRepo;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/xlsx")
    public void exportExcel(HttpServletResponse resp) throws IOException {
        List<Task> tasks = taskRepo.findAll();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet("Tasks & Reminders");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Task ID");
            header.createCell(1).setCellValue("Title");
            header.createCell(2).setCellValue("Description");
            header.createCell(3).setCellValue("Created");
            header.createCell(4).setCellValue("Due");
            header.createCell(5).setCellValue("Reminder ID");
            header.createCell(6).setCellValue("Reminder Message");

            int rowIdx = 1;
            for (Task t : tasks) {
                ReminderResponse[] rems = restTemplate.getForObject(
                        "http://localhost:8081/reminders/{taskId}",
                        ReminderResponse[].class,
                        t.taskId()
                );

                if (rems == null || rems.length == 0) {
                    var row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(t.taskId().toString());
                    row.createCell(1).setCellValue(t.taskTitle());
                    row.createCell(2).setCellValue(t.taskDescription());
                    row.createCell(3).setCellValue(t.taskCreationDate().toString());
                    row.createCell(4).setCellValue(t.taskDueDate().toString());
                } else {
                    for (ReminderResponse rm : rems) {
                        var row = sheet.createRow(rowIdx++);
                        row.createCell(0).setCellValue(t.taskId().toString());
                        row.createCell(1).setCellValue(t.taskTitle());
                        row.createCell(2).setCellValue(t.taskDescription());
                        row.createCell(3).setCellValue(t.taskCreationDate().toString());
                        row.createCell(4).setCellValue(t.taskDueDate().toString());
                        row.createCell(5).setCellValue(rm.reminderId());
                        row.createCell(6).setCellValue(rm.message());
                    }
                }
            }

            resp.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );
            resp.setHeader(
                    "Content-Disposition",
                    "attachment; filename=\"export.xlsx\""
            );

            wb.write(resp.getOutputStream());
            log.info("Excel report generated successfully ({} tasks, {} rows)",
                    tasks.size(), rowIdx - 1);
        }
    }
}
