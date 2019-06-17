package fr.cnes.regards.modules.storage.service;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.modules.templates.domain.Template;
import fr.cnes.regards.modules.templates.service.TemplateConfigUtil;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
public class StorageTemplateConfiguration {

    public static final String NOT_DISPATCHED_DATA_FILES_TEMPLATE_NAME = "NOT_DISPATCHED_DATA_FILES";

    public static final String NOT_SUBSETTED_DATA_FILES_TEMPLATE_NAME = "NOT_SUBSETTED_DATA_FILES";

    public static final String PARTIAL_DELETION_REPORT_TEMPLATE_NAME = "PARTIAL_DELETION_REPORT_TEMPLATE";

    @Bean
    public Template notDispatchedDataFiles() throws IOException {
        return TemplateConfigUtil.readTemplate(NOT_DISPATCHED_DATA_FILES_TEMPLATE_NAME,
                                               "template/not_dispatched_data_files_template.html");
    }

    @Bean
    public Template notSubsettedDataFiles() throws IOException {
        return TemplateConfigUtil.readTemplate(NOT_SUBSETTED_DATA_FILES_TEMPLATE_NAME,
                                               "template/not_subsetted_data_files_template.html");
    }

    @Bean
    public Template partialDeletionReport() throws IOException {
        return TemplateConfigUtil.readTemplate(PARTIAL_DELETION_REPORT_TEMPLATE_NAME,
                                               "template/partial_deletion_report_template.html");
    }

}
