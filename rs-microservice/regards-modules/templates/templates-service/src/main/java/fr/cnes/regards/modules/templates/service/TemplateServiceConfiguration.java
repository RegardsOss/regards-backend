/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.templates.service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fr.cnes.regards.modules.templates.domain.Template;
import fr.cnes.regards.modules.templates.domain.TemplatePathSubject;

/**
 * @author Xavier-Alexandre Brochard
 * @author Marc Sordi
 * @author oroussel for dismessness
 */
@Component
public class TemplateServiceConfiguration {

    /**
     * The email validation template code
     */
    public static final String EMAIL_ACCOUNT_VALIDATION_TEMPLATE_CODE = "EMAIL_ACCOUNT_VALIDATION_TEMPLATE";

    /**
     * The password reset template code
     */
    public static final String MDP_RESET_TEMPLATE_CODE = "PASSWORD_RESET_TEMPLATE";

    /**
     * The account unlock template code
     */
    public static final String ACCOUNT_UNLOCK_TEMPLATE_CODE = "ACCOUNT_UNLOCK_TEMPLATE";

    /**
     * The account refused template code
     */
    public static final String ACCOUNT_REFUSED_TEMPLATE_CODE = "ACCOUNT_REFUSED_TEMPLATE";

    /**
     * The password refused template code
     */
    public static final String PASSWORD_RESET_TEMPLATE_CODE = "PASSWORD_RESET_TEMPLATE_CODE";

    /**
     * The project user activated template code
     */
    public static final String PROJECT_USER_ACTIVATED_TEMPLATE_CODE = "PROJECT_USER_ACTIVATED_TEMPLATE";

    /**
     * The project user inactivated template code
     */
    public static final String PROJECT_USER_INACTIVATED_TEMPLATE_CODE = "PROJECT_USER_INACTIVATED_TEMPLATE";

    /**
     * The order created template code
     */
    public static final String ORDER_CREATED_TEMPLATE_CODE = "ORDER_CREATED_TEMPLATE";

    /**
     * The aside orders notification template code
     */
    public static final String ASIDE_ORDERS_NOTIFICATION_TEMPLATE_CODE = "ASIDE_ORDERS_NOTIFICATION_TEMPLATE";

    /**
     * The not dispatched data files template code
     */
    public static final String NOT_DISPATCHED_DATA_FILES_CODE = "NOT_DISPATCHED_DATA_FILES";

    /**
     * the not subsetted data files template code
     */
    public static final String NOT_SUBSETTED_DATA_FILES_CODE = "NOT_SUBSETTED_DATA_FILES";

    /**
     * List of template bean name
     */
    public static final String TEMPLATES = "templates";

    /**
     * The verification email template as html
     */
    private static final String EMAIL_ACCOUNT_VALIDATION_TEMPLATE = "template/email-account-validation-template.html";

    /**
     * The password reset email template as html
     */
    private static final String PASSWORD_RESET_TEMPLATE = "template/password-reset-template.html";

    /**
     * The account unlock email template as html
     */
    private static final String ACCOUNT_UNLOCK_TEMPLATE = "template/account-unlock-template.html";

    /**
     * The account refused email template as html
     */

    private static final String ACCOUNT_REFUSED_TEMPLATE = "template/account-refused-template.html";

    /**
     * The project user activated email template as html
     */
    private static final String PROJECT_USER_ACTIVATED_TEMPLATE = "template/project-user-activated-template.html";

    /**
     * The project user inactivated email template as html
     */
    private static final String PROJECT_USER_INACTIVATED_TEMPLATE = "template/project-user-inactivated-template.html";

    /**
     * The order created email template as html
     */
    private static final String ORDER_CREATED_TEMPLATE = "template/order-created-template.html";

    /**
     * The aside blablah... (guess the end)
     */
    private static final String ASIDE_ORDERS_NOTIFICATION_TEMPLATE = "template/aside-orders-notification-template.html";

    /**
     * The not dispatched data files template as html
     */
    private static final String NOT_DISPATCHED_DATA_FILES_TEMPLATE = "template/not_dispatched_data_files_template.html";

    /**
     * The not subsetted data files template as html
     */
    private static final String NOT_SUBSETTED_DATA_FILES_TEMPLATE = "template/not_subsetted_data_files_template.html";

    private static final Map<String, TemplatePathSubject> templateCodePathMap = Maps.newHashMap();

    private static void addTemplate(String templateCode, String templatePath, String emailSubject) {
        templateCodePathMap.put(templateCode, new TemplatePathSubject(templatePath, emailSubject));
    }

    @PostConstruct
    public void postConstruct() {
        templateCodePathMap.put(EMAIL_ACCOUNT_VALIDATION_TEMPLATE_CODE,
                                new TemplatePathSubject(EMAIL_ACCOUNT_VALIDATION_TEMPLATE, "Account Confirmation"));
        templateCodePathMap.put(PROJECT_USER_ACTIVATED_TEMPLATE_CODE,
                                new TemplatePathSubject(PROJECT_USER_ACTIVATED_TEMPLATE, "Access re-activated"));
        templateCodePathMap.put(PROJECT_USER_INACTIVATED_TEMPLATE_CODE,
                                new TemplatePathSubject(PROJECT_USER_INACTIVATED_TEMPLATE, "Access deactivated"));
        templateCodePathMap
                .put(ORDER_CREATED_TEMPLATE_CODE, new TemplatePathSubject(ORDER_CREATED_TEMPLATE, "Order created"));
        templateCodePathMap.put(ASIDE_ORDERS_NOTIFICATION_TEMPLATE_CODE,
                                new TemplatePathSubject(ASIDE_ORDERS_NOTIFICATION_TEMPLATE, "Orders waiting"));
        templateCodePathMap.put(NOT_DISPATCHED_DATA_FILES_CODE,
                                new TemplatePathSubject(NOT_DISPATCHED_DATA_FILES_TEMPLATE,
                                                        "Files not associated to any data storages"));
        templateCodePathMap.put(NOT_SUBSETTED_DATA_FILES_CODE,
                                new TemplatePathSubject(NOT_SUBSETTED_DATA_FILES_TEMPLATE,
                                                        "Files could not be handled by their data storage"));
        templateCodePathMap.put(ACCOUNT_UNLOCK_TEMPLATE_CODE,
                                new TemplatePathSubject(ACCOUNT_UNLOCK_TEMPLATE, "Account Unlock"));
        templateCodePathMap.put(ACCOUNT_REFUSED_TEMPLATE_CODE,
                                new TemplatePathSubject(ACCOUNT_REFUSED_TEMPLATE, "Account refused"));
        templateCodePathMap.put(PASSWORD_RESET_TEMPLATE_CODE,
                                new TemplatePathSubject(PASSWORD_RESET_TEMPLATE, "Password Reset"));
    }

    /**
     * @return the list of initialized template bean for the current microservice
     * @throws IOException
     */
    @Bean(name = TEMPLATES)
    public List<Template> templates() throws IOException {
        List<Template> templates = Lists.newArrayList();
        for (Map.Entry<String, TemplatePathSubject> templateCodePathEntry : templateCodePathMap.entrySet()) {
            ClassPathResource resource = new ClassPathResource(templateCodePathEntry.getValue().getTemplatePath());
            try (InputStream is = resource.getInputStream()) {
                final String text = inputStreamToString(is);
                final Map<String, String> dataStructure = new HashMap<>();
                templates.add(new Template(templateCodePathEntry.getKey(),
                                           text,
                                           dataStructure,
                                           templateCodePathEntry.getValue().getEmailSubject()));
            } catch (FileNotFoundException fnfe) {
                // due to code construction, it happens and it is not an error or an issue
            }
        }
        return templates;
    }

    /**
     * Writes an {@link InputStream} to a {@link String}.
     * @param is the input stream
     * @return the string
     * @throws IOException when an error occurs while reading the stream
     */
    private String inputStreamToString(final InputStream is) throws IOException {
        final StringBuilder buf = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buf.append(line);
            }
            return buf.toString();
        }
    }

}
