package org.taskservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.taskservice.service.ExportService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExportService exportService;

    @InjectMocks
    private ExportController exportController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(exportController).build();
    }

    @Test
    void exportTasksToExcel_returnsExcelFileWithCorrectHeaders() throws Exception {
        byte[] dummyExcel = "fake-excel-content".getBytes();
        when(exportService.generateExport()).thenReturn(dummyExcel);

        mockMvc.perform(get("/exports/xlsx"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(dummyExcel))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment; filename=tasks_and_reminders_")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));


    }
}
