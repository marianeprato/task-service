package org.taskservice.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.taskservice.model.Task;
import org.taskservice.repository.TaskRepository;
import org.taskservice.service.export.ExportResult;
import org.taskservice.service.export.ExportService;
import org.taskservice.service.export.ExportStrategy;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private TaskRepository mockTaskRepository;

    @Mock
    private ExportStrategy mockXlsxStrategy;

    @Mock
    private ExportStrategy mockCsvStrategy;

    @Mock
    private ExportStrategy mockPdfStrategy;

    private ExportService exportService;

    private Map<String, ExportStrategy> strategyMap;

    @BeforeEach
    void setUp() {
        strategyMap = Map.of(
                "xlsxExportStrategy", mockXlsxStrategy,
                "csvExportStrategy", mockCsvStrategy,
                "pdfExportStrategy", mockPdfStrategy);
        exportService = new ExportService(mockTaskRepository, strategyMap);
    }

    private static Stream<Arguments> strategyProvider() {
        return Stream.of(
                Arguments.of("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
                Arguments.of("csv", "text/plain", ".csv"),
                Arguments.of("pdf", "application/pdf", ".pdf"));
    }

    @ParameterizedTest
    @MethodSource("strategyProvider")
    void shouldSelectAndExecuteCorrectStrategy(String format, String mediaTypeString, String expectedExtension) {
        String strategyName = format + "ExportStrategy";
        ExportStrategy mockStrategy = strategyMap.get(strategyName);
        MediaType expectedMediaType = MediaType.parseMediaType(mediaTypeString);

        List<Task> tasks = Collections.singletonList(mock(Task.class));
        byte[] expectedContent = (format + "-data").getBytes();

        when(mockTaskRepository.findAll()).thenReturn(tasks);
        when(mockStrategy.export(tasks)).thenReturn(expectedContent);
        when(mockStrategy.getMediaType()).thenReturn(expectedMediaType);
        when(mockStrategy.getFileExtension()).thenReturn(expectedExtension);

        ExportResult result = exportService.generateExport(format);

        assertNotNull(result);
        assertArrayEquals(expectedContent, result.content());
        assertEquals(expectedExtension, result.fileExtension());
        assertEquals(expectedMediaType, result.mediaType());

        verify(mockStrategy, times(1)).export(tasks);

        strategyMap.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(strategyName))
                .forEach(entry -> verify(entry.getValue(), never()).export(any()));
    }

    @Test
    void shouldThrowExceptionWhenFormatIsUnsupported() {
        String unsupportedFormat = "unsupported";

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> exportService.generateExport(unsupportedFormat));

        assertEquals("Unsupported export format: " + unsupportedFormat, exception.getMessage());
    }
}
