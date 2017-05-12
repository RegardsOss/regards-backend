/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.templates.domain.Template;

/**
 * @author Xavier-Alexandre Brochard
 * @author Marc Sordi
 */
@Component
public class TemplateServiceConfiguration {

    /**
     * The email validation template code
     */
    public static final String EMAIL_ACCOUNT_VALIDATION_TEMPLATE_CODE = "emailAccountValidationTemplate";

    /**
     * The password reset template code
     */
    public static final String MDP_RESET_TEMPLATE = "passwordResetTemplate";

    /**
     * The account unlock template code
     */
    public static final String ACCOUNT_UNLOCK_TEMPLATE = "accountUnlockTemplate";

    /**
     * The email validation template as html
     */
    @Value("classpath:email-validation-template.html")
    private Resource emailValidationTemplate;

    /**
     * The password reset template as html
     */
    @Value("classpath:password-reset-template.html")
    private Resource passwordResetTemplate;

    /**
     * The password reset template as html
     */
    @Value("classpath:account-unlock-template.html")
    private Resource accountUnlockTemplate;

    /**
     * The password reset template as html
     */
    @Value("classpath:email-account-validation-template.html")
    private Resource emailAccountValidationTemplate;

    @Bean
    public Template emailAccountValidationTemplate() throws IOException {
        try (InputStream is = emailAccountValidationTemplate.getInputStream()) {
            final String text = inputStreamToString(is);
            final Map<String, String> dataStructure = new HashMap<>();
            return new Template(EMAIL_ACCOUNT_VALIDATION_TEMPLATE_CODE, text, dataStructure, "Account Confirmation");
        }
    }

    @Bean
    public Template passwordResetTemplate() throws IOException {
        try (InputStream is = passwordResetTemplate.getInputStream()) {
            final String text = inputStreamToString(is);
            final Map<String, String> dataStructure = new HashMap<>();
            return new Template(MDP_RESET_TEMPLATE, text, dataStructure, "Password Reset");
        }
    }

    @Bean
    public Template accountUnlockTemplate() throws IOException {
        try (InputStream is = accountUnlockTemplate.getInputStream()) {
            final String text = inputStreamToString(is);
            final Map<String, String> dataStructure = new HashMap<>();
            return new Template(ACCOUNT_UNLOCK_TEMPLATE, text, dataStructure, "Account Unlock");
        }
    }

    /**
     * Writes an {@link InputStream} to a {@link String}.
     *
     * @param pInputStream
     *            the input stream
     * @return the string
     * @throws IOException
     *             when an error occurs while reading the stream
     */
    private String inputStreamToString(final InputStream pInputStream) throws IOException {
        final StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(
                new InputStreamReader(pInputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
            return textBuilder.toString();
        }
    }

}
