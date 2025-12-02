package org.taskservice.service.export;

import java.util.List;

import org.springframework.http.MediaType;
import org.taskservice.model.Task;

public interface ExportStrategy {

    byte[] export(List<Task> tasks);

    String getFileExtension();

    MediaType getMediaType();
}
