/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.amazonaws.util.IOUtils;

import fr.cnes.regards.modules.templates.domain.Template;

/**
 *
 * @author Xavier-Alexandre Brochard
 */
@Configuration
public class TemplateServiceConfiguration {

    /**
     * The email validation template code
     */
    private static final String EMAIL_VALIDATION_TEMPLATE_CODE = "emailValidationTemplate";

    /**
     * The email validation template as html
     */
    @Value("classpath:email-validation-template.html")
    private Resource emailValidationTemplate;

    @Bean
    public Template emailValidationTemplate() throws IOException {
        try (InputStream is = emailValidationTemplate.getInputStream()) {
            final String text = IOUtils.toString(is);
            final Map<String, String> dataStructure = new HashMap<>();
            return new Template(EMAIL_VALIDATION_TEMPLATE_CODE, text, dataStructure, "Access Confirmation");
        }
    }

}
