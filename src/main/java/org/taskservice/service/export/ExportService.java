package org.taskservice.service.export;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.taskservice.model.Task;
import org.taskservice.repository.TaskRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ExportService {

    private final TaskRepository taskRepository;
    private final Map<String, ExportStrategy> strategyMap;

    public ExportResult generateExport(String format) {
        String strategyName = format.toLowerCase() + "ExportStrategy";

        ExportStrategy strategy = strategyMap.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported export format: " + format);
        }
        List<Task> tasks = taskRepository.findAll();
        byte[] content = strategy.export(tasks);
        return new ExportResult(content, strategy.getMediaType(), strategy.getFileExtension());
    }
}
