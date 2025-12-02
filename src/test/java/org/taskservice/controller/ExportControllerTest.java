package org.taskservice.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.taskservice.service.export.ExportResult;
import org.taskservice.service.export.ExportService;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExportService mockExportService;

    @InjectMocks
    private ExportController exportController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(exportController).build();
    }

    @Test
    void shouldReturnPdfFileWhenFormatIsPdf() throws Exception {
        String format = "pdf";
        byte[] pdfContent = "pdf-content".getBytes();
        ExportResult exportResult = new ExportResult(pdfContent, MediaType.APPLICATION_PDF, ".pdf");

        when(mockExportService.generateExport(format)).thenReturn(exportResult);

        mockMvc.perform(get("/exports/{format}", format))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString(".pdf")))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    void shouldReturnBadRequestWhenFormatIsUnsupported() throws Exception {
        String unsupportedFormat = "unsupported";

        when(mockExportService.generateExport(unsupportedFormat))
                .thenThrow(new IllegalArgumentException("Unsupported format: " + unsupportedFormat));

        mockMvc.perform(get("/exports/{format}", unsupportedFormat)).andExpect(status().isBadRequest());
    }
}
