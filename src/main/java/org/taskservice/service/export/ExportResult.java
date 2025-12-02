package org.taskservice.service.export;

import org.springframework.http.MediaType;

public record ExportResult(byte[] content, MediaType mediaType, String fileExtension) {}
