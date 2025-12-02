package org.taskservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.taskservice.client.ReminderClient;
import org.taskservice.service.export.CsvExportStrategy;
import org.taskservice.service.export.ExcelExportStrategy;
import org.taskservice.service.export.ExportStrategy;
import org.taskservice.service.export.PdfExportStrategy;

@Configuration
public class ExportStrategyConfig {

    @Bean
    public ExportStrategy xlsxExportStrategy(ReminderClient reminderClient) {
        return new ExcelExportStrategy(reminderClient);
    }

    @Bean
    public ExportStrategy csvExportStrategy(ReminderClient reminderClient) {
        return new CsvExportStrategy(reminderClient);
    }

    @Bean
    public ExportStrategy pdfExportStrategy(ReminderClient reminderClient) {
        return new PdfExportStrategy(reminderClient);
    }
}
