package org.taskservice.service;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.taskservice.client.ReminderClient;
import org.taskservice.dto.ReminderResponse;
import org.taskservice.model.Task;
import org.taskservice.repository.TaskRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ReminderClient reminderClient;

    @InjectMocks
    private ExportService exportService;

    private static Stream<Arguments> reminderScenarios() {
        final Task task1 = new Task(UUID.randomUUID(), "Test Task", "Test Desc", LocalDate.now(), LocalDate.now().plusDays(5));
        final ReminderResponse r1 = new ReminderResponse(1L, task1.taskId(), "First reminder");
        final ReminderResponse r2 = new ReminderResponse(2L, task1.taskId(), "Second reminder");

        final Task task2 = new Task(UUID.randomUUID(), "Test Task", "Test Desc", LocalDate.now(), LocalDate.now().plusDays(5));
        final ReminderResponse r3 = new ReminderResponse(1L, task2.taskId(), "First reminder");

        final Task task3 = new Task(UUID.randomUUID(), "Test Task", "Test Desc", LocalDate.now(), LocalDate.now().plusDays(5));

        return Stream.of(
                arguments(task3, List.<ReminderResponse>of(), 1, null, null),
                arguments(task2, List.of(r3), 1, 1L, "First reminder"),
                arguments(task1, List.of(r1, r2), 2, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("reminderScenarios")
    void generatesExcelForVariousReminderScenarios(
            final Task task,
            final List<ReminderResponse> reminders,
            final int expectedLastRowNum,
            final Long expectedReminderId,
            final String expectedReminderMsg
    ) throws Exception {
        when(taskRepository.findAll()).thenReturn(List.of(task));
        when(reminderClient.getRemindersForTask(task.taskId())).thenReturn(reminders);

        final byte[] data = exportService.generateExport();
        try (final XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            final var sheet = wb.getSheet("Tasks & Reminders");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getLastRowNum()).isEqualTo(expectedLastRowNum);

            if (expectedReminderId != null) {
                final var row = sheet.getRow(1);
                assertThat((long) row.getCell(5).getNumericCellValue()).isEqualTo(expectedReminderId);
                assertThat(row.getCell(6).getStringCellValue()).isEqualTo(expectedReminderMsg);
            }
        }
    }

    private static Stream<Arguments> headerCells() {
        return Stream.of(
                arguments(0, "Task ID"),
                arguments(1, "Title"),
                arguments(2, "Description"),
                arguments(3, "Created"),
                arguments(4, "Due"),
                arguments(5, "Reminder ID"),
                arguments(6, "Reminder Message")
        );
    }

    @ParameterizedTest
    @MethodSource("headerCells")
    void headerCellIsCorrect(
            final int idx,
            final String expected
    ) throws Exception {
        final Task task = new Task(UUID.randomUUID(), "X", "Y", LocalDate.now(), LocalDate.now());
        when(taskRepository.findAll()).thenReturn(List.of(task));
        when(reminderClient.getRemindersForTask(task.taskId())).thenReturn(List.of());

        final byte[] data = exportService.generateExport();
        try (final XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            final var cell = wb.getSheet("Tasks & Reminders").getRow(0).getCell(idx);
            assertThat(cell.getStringCellValue()).isEqualTo(expected);
        }
    }

    @Test
    void generateExportReturnsNonEmptyByteArray() {
        final Task task = new Task(UUID.randomUUID(), "Test Task", "Test Desc", LocalDate.now(), LocalDate.now().plusDays(5));
        when(taskRepository.findAll()).thenReturn(List.of(task));
        when(reminderClient.getRemindersForTask(task.taskId())).thenReturn(List.of());

        final byte[] result = exportService.generateExport();
        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    void generatesExcelWhenNoTasks() throws Exception {
        when(taskRepository.findAll()).thenReturn(List.of());

        final byte[] data = exportService.generateExport();
        try (final XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            final var sheet = wb.getSheet("Tasks & Reminders");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getLastRowNum()).isEqualTo(0);
        }
    }

    @Test
    void generatesExcelWithMultipleTasksMixedReminders() throws Exception {
        final Task taskA = new Task(UUID.randomUUID(), "Task A", "Desc A", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2));
        final Task taskB = new Task(UUID.randomUUID(), "Task B", "Desc B", LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 2));
        final ReminderResponse b1 = new ReminderResponse(10L, taskB.taskId(), "Rem B1");
        final ReminderResponse b2 = new ReminderResponse(20L, taskB.taskId(), "Rem B2");

        when(taskRepository.findAll()).thenReturn(List.of(taskA, taskB));
        when(reminderClient.getRemindersForTask(taskA.taskId())).thenReturn(List.of());
        when(reminderClient.getRemindersForTask(taskB.taskId())).thenReturn(List.of(b1, b2));

        final byte[] data = exportService.generateExport();
        try (final XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            final var sheet = wb.getSheet("Tasks & Reminders");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getLastRowNum()).isEqualTo(3);

            final var rowA = sheet.getRow(1);
            assertThat(rowA.getCell(1).getStringCellValue()).isEqualTo("Task A");
            assertThat(rowA.getCell(6)).isNull();

            final var rowB1 = sheet.getRow(2);
            assertThat(rowB1.getCell(1).getStringCellValue()).isEqualTo("Task B");
            assertThat((long) rowB1.getCell(5).getNumericCellValue()).isEqualTo(10L);
            assertThat(rowB1.getCell(6).getStringCellValue()).isEqualTo("Rem B1");

            final var rowB2 = sheet.getRow(3);
            assertThat(rowB2.getCell(1).getStringCellValue()).isEqualTo("Task B");
            assertThat((long) rowB2.getCell(5).getNumericCellValue()).isEqualTo(20L);
            assertThat(rowB2.getCell(6).getStringCellValue()).isEqualTo("Rem B2");
        }
    }

    @Test
    void verifiesTaskFieldsArePopulatedCorrectly() throws Exception {
        final UUID id = UUID.randomUUID();
        final LocalDate created = LocalDate.of(2025, 3, 3);
        final LocalDate due = LocalDate.of(2025, 3, 10);
        final Task task = new Task(id, "PopTest", "PopDesc", created, due);

        when(taskRepository.findAll()).thenReturn(List.of(task));
        when(reminderClient.getRemindersForTask(task.taskId())).thenReturn(List.of());

        final byte[] data = exportService.generateExport();
        try (final XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            final var row = wb.getSheet("Tasks & Reminders").getRow(1);

            assertThat(row.getCell(0).getStringCellValue()).isEqualTo(id.toString());
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("PopTest");
            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("PopDesc");
            assertThat(row.getCell(3).getStringCellValue()).isEqualTo(created.toString());
            assertThat(row.getCell(4).getStringCellValue()).isEqualTo(due.toString());
        }
    }

    private enum FailureScenario {
        REMINDER("Reminder service down"),
        WRITE("Failed to generate Excel file");

        private final String messageFragment;

        FailureScenario(final String messageFragment) {
            this.messageFragment = messageFragment;
        }

        String getMessageFragment() {
            return messageFragment;
        }
    }

    @ParameterizedTest
    @EnumSource(FailureScenario.class)
    void generateExportFails(final FailureScenario scenario) throws Exception {
        final Task task = new Task(UUID.randomUUID(), "ErrTest", "ErrDesc", LocalDate.now(), LocalDate.now().plusDays(1));
        when(taskRepository.findAll()).thenReturn(List.of(task));

        if (scenario == FailureScenario.REMINDER) {
            when(reminderClient.getRemindersForTask(task.taskId()))
                    .thenThrow(new RuntimeException("Reminder service down"));
            assertThatThrownBy(exportService::generateExport)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(scenario.getMessageFragment());
        } else {
            final ExportService spyService = spy(new ExportService(taskRepository, reminderClient));
            doThrow(new IOException("Disk full")).when(spyService).writeWorkbook(any(), any());
            assertThatThrownBy(spyService::generateExport)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(scenario.getMessageFragment());
        }
    }
}
