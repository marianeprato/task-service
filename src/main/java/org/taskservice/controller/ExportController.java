package org.taskservice.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.taskservice.service.export.ExportResult;
import org.taskservice.service.export.ExportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/exports")
@RequiredArgsConstructor
public class ExportController {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ExportService exportService;

    @GetMapping("/{format}")
    public ResponseEntity<byte[]> exportTasks(@PathVariable String format) {
        log.info("Export endpoint called for format: {}", format);

        ExportResult result = exportService.generateExport(format);

        String filename = buildFileName(result.fileExtension());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(result.mediaType())
                .body(result.content());
    }

    private static String buildFileName(String fileExtension) {
        return "tasks_and_reminders_" + LocalDateTime.now().format(TIMESTAMP_FMT) + fileExtension;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
