package fr.cnes.regards.modules.ingest.service.conf;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.modules.templates.domain.Template;
import fr.cnes.regards.modules.templates.service.TemplateConfigUtil;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
public class IngestTemplateConfiguration {

    public static final String REJECTED_SIPS_TEMPLATE_NAME = "REJECTED_SIPS_TEMPLATE";

    @Bean
    public Template rejectedSipsTemplate() throws IOException {
        return TemplateConfigUtil.readTemplate(REJECTED_SIPS_TEMPLATE_NAME, "template/rejected_sips_template.html");
    }
}
