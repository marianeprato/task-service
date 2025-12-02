package org.taskservice.service.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.taskservice.client.ReminderClient;
import org.taskservice.model.Task;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

public class PdfExportStrategy implements ExportStrategy {

    private final ReminderClient reminderClient;

    public PdfExportStrategy(ReminderClient reminderClient) {
        this.reminderClient = reminderClient;
    }

    @Override
    public byte[] export(List<Task> tasks) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Tasks Report").setBold().setFontSize(18));

            float[] columWidths = {4, 5, 3, 3};
            Table table = new Table(columWidths);

            table.addHeaderCell(new Cell().add(new Paragraph("Task ID").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Title").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Due Date").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Priority").setBold()));

            for (Task task : tasks) {
                table.addCell(new Cell().add(new Paragraph(task.taskId().toString())));
                table.addCell(new Cell().add(new Paragraph(task.taskTitle())));
                table.addCell(new Cell().add(new Paragraph(task.taskDueDate().toString())));
                table.addCell(new Cell().add(new Paragraph(task.priority().getValue())));
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF file", e);
        }
    }

    @Override
    public String getFileExtension() {
        return ".pdf";
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.APPLICATION_PDF;
    }
}
