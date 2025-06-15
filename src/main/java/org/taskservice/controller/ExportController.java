package org.taskservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.taskservice.service.ExportService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/exports")
@RequiredArgsConstructor
public class ExportController {

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ExportService exportService;

    @GetMapping("/xlsx")
    public ResponseEntity<byte[]> exportTasksToExcel() {
        log.info("Export endpoint /xlsx called");

        final byte[] excelFile = exportService.generateExport();
        final String filename = buildFileName();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelFile);
    }

    private static String buildFileName() {
        return "tasks_and_reminders_"
                + LocalDateTime.now().format(TIMESTAMP_FMT)
                + ".xlsx";
    }
}
