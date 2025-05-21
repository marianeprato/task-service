package org.taskservice.controller;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.taskservice.dto.ReminderResponse;
import org.taskservice.model.Task;
import org.taskservice.repository.TaskRepository;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TaskRepository taskRepo;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExportController exportController;

    private Task sampleTask;
    private ReminderResponse[] sampleReminders;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(exportController).build();

        sampleTask = new Task(
                UUID.randomUUID(),
                "Clean code",
                "Refactor export logic",
                LocalDate.of(2025, 5, 21),
                LocalDate.of(2025, 5, 25)
        );

        sampleReminders = new ReminderResponse[] {
                new ReminderResponse(42L, sampleTask.taskId(), "Reminder for task: Clean code")
        };
    }

    @Test
    void exportXlsx_withReminders_shouldReturnCombinedSheet() throws Exception {
        when(taskRepo.findAll()).thenReturn(List.of(sampleTask));
        when(restTemplate.getForObject(
                eq("http://localhost:8081/reminders/{taskId}"),
                eq(ReminderResponse[].class),
                eq(sampleTask.taskId())
        )).thenReturn(sampleReminders);

        MvcResult result = mockMvc.perform(get("/export/xlsx"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"export.xlsx\""
                ))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = wb.getSheet("Tasks & Reminders");
            assertThat(sheet).isNotNull();

            var header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Task ID");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Title");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("Description");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("Created");
            assertThat(header.getCell(4).getStringCellValue()).isEqualTo("Due");
            assertThat(header.getCell(5).getStringCellValue()).isEqualTo("Reminder ID");
            assertThat(header.getCell(6).getStringCellValue()).isEqualTo("Reminder Message");

            var data = sheet.getRow(1);
            assertThat(data.getCell(0).getStringCellValue())
                    .isEqualTo(sampleTask.taskId().toString());
            assertThat(data.getCell(1).getStringCellValue())
                    .isEqualTo(sampleTask.taskTitle());
            assertThat(data.getCell(2).getStringCellValue())
                    .isEqualTo(sampleTask.taskDescription());
            assertThat(data.getCell(3).getStringCellValue())
                    .isEqualTo(sampleTask.taskCreationDate().toString());
            assertThat(data.getCell(4).getStringCellValue())
                    .isEqualTo(sampleTask.taskDueDate().toString());
            assertThat((long)data.getCell(5).getNumericCellValue())
                    .isEqualTo(sampleReminders[0].reminderId());
            assertThat(data.getCell(6).getStringCellValue())
                    .isEqualTo(sampleReminders[0].message());
        }
    }

    @Test
    void exportXlsx_withoutReminders_shouldLeaveReminderColumnsBlankOrNull() throws Exception {
        when(taskRepo.findAll()).thenReturn(List.of(sampleTask));
        when(restTemplate.getForObject(
                eq("http://localhost:8081/reminders/{taskId}"),
                eq(ReminderResponse[].class),
                eq(sampleTask.taskId())
        )).thenReturn(new ReminderResponse[0]);

        MvcResult result = mockMvc.perform(get("/export/xlsx"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = wb.getSheet("Tasks & Reminders");
            var data = sheet.getRow(1);

            XSSFCell cell5 = data.getCell(5);
            XSSFCell cell6 = data.getCell(6);

            if (cell5 != null) {
                assertThat(cell5.getStringCellValue()).isEmpty();
            }
            if (cell6 != null) {
                assertThat(cell6.getStringCellValue()).isEmpty();
            }
        }
    }
}
