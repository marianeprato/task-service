package org.taskservice.service;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.taskservice.client.ReminderClient;
import org.taskservice.dto.ReminderResponse;
import org.taskservice.model.Task;
import org.taskservice.repository.TaskRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ReminderClient reminderClient;

    @InjectMocks
    private ExportService exportService;

    private Task task;
    private ReminderResponse reminder1;
    private ReminderResponse reminder2;

    @BeforeEach
    void setUp() {
        UUID taskId = UUID.randomUUID();
        task = new Task(taskId, "Test Task", "Test Desc", LocalDate.now(), LocalDate.now().plusDays(5));
        reminder1 = new ReminderResponse(1L, taskId, "First reminder");
        reminder2 = new ReminderResponse(2L, taskId, "Second reminder");
    }

    @Test
    void generatesExcelWithoutReminders() throws Exception {
        when(taskRepository.findAll()).thenReturn(List.of(task));
        when(reminderClient.getRemindersForTask(task.taskId())).thenReturn(List.of());

        byte[] data = exportService.generateExport();
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            var sheet = wb.getSheet("Tasks & Reminders");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getLastRowNum()).isEqualTo(1);

            var row = sheet.getRow(1);
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("Test Task");
            assertThat(row.getCell(6)).isNull();
        }
    }

    @Test
    void generatesExcelWithOneReminder() throws Exception {
        when(taskRepository.findAll()).thenReturn(List.of(task));
        when(reminderClient.getRemindersForTask(task.taskId())).thenReturn(List.of(reminder1));

        byte[] data = exportService.generateExport();
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            var row = wb.getSheet("Tasks & Reminders").getRow(1);
            assertThat((long) row.getCell(5).getNumericCellValue()).isEqualTo(reminder1.reminderId());
            assertThat(row.getCell(6).getStringCellValue()).isEqualTo(reminder1.message());
        }
    }

    @Test
    void generatesExcelWithMultipleReminders() throws Exception {
        when(taskRepository.findAll()).thenReturn(List.of(task));
        when(reminderClient.getRemindersForTask(task.taskId())).thenReturn(List.of(reminder1, reminder2));

        byte[] data = exportService.generateExport();
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            var sheet = wb.getSheet("Tasks & Reminders");
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    void throwsExceptionOnWriteFailure() throws IOException {
        ExportService spyService = Mockito.spy(new ExportService(taskRepository, reminderClient));

        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Test", "Desc", LocalDate.now(), LocalDate.now().plusDays(1));
        when(taskRepository.findAll()).thenReturn(List.of(task));
        when(reminderClient.getRemindersForTask(taskId)).thenReturn(List.of());

        doThrow(new IOException("Disk full"))
                .when(spyService).writeWorkbook(any(XSSFWorkbook.class), any(ByteArrayOutputStream.class));

        assertThatThrownBy(spyService::generateExport)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate Excel file");
    }
}
