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

/**
 * @author Xavier-Alexandre Brochard
 * @author Marc Sordi
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
     * The not subsetted data files template code
     */
    public static final String NOT_DISPATCHED_DATA_FILES_CODE = "NOT_DISPATCHED_DATA_FILES";

    public static final String NOT_SUBSETTED_DATA_FILES_CODE = "NOT_SUBSETTED_DATA_FILES";

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

    private static final String NOT_DISPATCHED_DATA_FILES_TEMPLATE = "template/not_dispatched_data_files_template.html";

    private static final String NOT_SUBSETTED_DATA_FILES_TEMPLATE = "template/not_subsetted_data_files_template.html";

    private static final Map<String, String> templateCodePathMap = Maps.newHashMap();

    public static final String TEMPLATES = "templates";

    @PostConstruct
    public void postConstruct() {
        templateCodePathMap.put(EMAIL_ACCOUNT_VALIDATION_TEMPLATE_CODE, EMAIL_ACCOUNT_VALIDATION_TEMPLATE);
        templateCodePathMap.put(PROJECT_USER_ACTIVATED_TEMPLATE_CODE, PROJECT_USER_ACTIVATED_TEMPLATE);
        templateCodePathMap.put(PROJECT_USER_INACTIVATED_TEMPLATE_CODE, PROJECT_USER_INACTIVATED_TEMPLATE);
        templateCodePathMap.put(ORDER_CREATED_TEMPLATE_CODE, ORDER_CREATED_TEMPLATE);
        templateCodePathMap.put(ASIDE_ORDERS_NOTIFICATION_TEMPLATE_CODE, ASIDE_ORDERS_NOTIFICATION_TEMPLATE);
    }

    private static void addTemplate(String templateCode, String templatePath) {
        templateCodePathMap.put(templateCode, templatePath);
    }

//    /**
//     * Declare the template as bean
//     * @return the template
//     */
//    @Bean
//    public Template emailAccountValidationTemplate() throws IOException {
//        ClassPathResource resource = new ClassPathResource(EMAIL_ACCOUNT_VALIDATION_TEMPLATE);
//        try (InputStream is = resource.getInputStream()) {
//            final String text = inputStreamToString(is);
//            final Map<String, String> dataStructure = new HashMap<>();
//            return new Template(EMAIL_ACCOUNT_VALIDATION_TEMPLATE_CODE, text, dataStructure, "Account Confirmation");
//        } catch (FileNotFoundException fnfe) {
//            return null;
//        }
//    }

    /**
     * Declare the template as bean
     * @return the template
     */
    @Bean
    public Template passwordResetTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource(PASSWORD_RESET_TEMPLATE);
        try (InputStream is = resource.getInputStream()) {
            final String text = inputStreamToString(is);
            final Map<String, String> dataStructure = new HashMap<>();
            return new Template(MDP_RESET_TEMPLATE_CODE, text, dataStructure, "Password Reset");
        } catch (FileNotFoundException fnfe) {
            return null;
        }
    }

    /**
     * Declare the template as bean
     * @return the template
     */
    @Bean
    public Template accountUnlockTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource(ACCOUNT_UNLOCK_TEMPLATE);
        try (InputStream is = resource.getInputStream()) {
            final String text = inputStreamToString(is);
            final Map<String, String> dataStructure = new HashMap<>();
            return new Template(ACCOUNT_UNLOCK_TEMPLATE_CODE, text, dataStructure, "Account Unlock");
        } catch (FileNotFoundException fnfe) {
            return null;
        }
    }

    /**
     * Declare the template as bean
     * @return the template
     */
    @Bean
    public Template accountRefusedTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource(ACCOUNT_REFUSED_TEMPLATE);
        try (InputStream is = resource.getInputStream()) {
            final String text = inputStreamToString(is);
            final Map<String, String> dataStructure = new HashMap<>();
            return new Template(ACCOUNT_REFUSED_TEMPLATE_CODE, text, dataStructure, "Account refused");
        } catch (FileNotFoundException fnfe) {
            return null;
        }
    }

//    /**
//     * Declare the template as bean
//     * @return the template
//     */
//    @Bean
//    public Template projectUserActivatedTemplate() throws IOException {
//        ClassPathResource resource = new ClassPathResource(PROJECT_USER_ACTIVATED_TEMPLATE);
//        try (InputStream is = resource.getInputStream()) {
//            final String text = inputStreamToString(is);
//            final Map<String, String> dataStructure = new HashMap<>();
//            return new Template(PROJECT_USER_ACTIVATED_TEMPLATE_CODE, text, dataStructure, "Access re-activated");
//        } catch (FileNotFoundException fnfe) {
//            return null;
//        }
//    }
//
//    /**
//     * Declare the template as bean
//     * @return the template
//     */
//    @Bean
//    public Template projectUserInactivatedTemplate() throws IOException {
//        ClassPathResource resource = new ClassPathResource(PROJECT_USER_INACTIVATED_TEMPLATE);
//        try (InputStream is = resource.getInputStream()) {
//            final String text = inputStreamToString(is);
//            final Map<String, String> dataStructure = new HashMap<>();
//            return new Template(PROJECT_USER_INACTIVATED_TEMPLATE_CODE, text, dataStructure, "Access deactivated");
//        } catch (FileNotFoundException fnfe) {
//            return null;
//        }
//    }
//
//    @Bean
//    public Template orderCreatedTemplate() throws IOException {
//        ClassPathResource resource = new ClassPathResource(ORDER_CREATED_TEMPLATE);
//        try (InputStream is = resource.getInputStream()) {
//            final String text = inputStreamToString(is);
//            final Map<String, String> dataStructure = new HashMap<>();
//            return new Template(ORDER_CREATED_TEMPLATE_CODE, text, dataStructure, "Order created");
//        } catch (FileNotFoundException fnfe) {
//            return null;
//        }
//    }
//
//    @Bean
//    public Template asideOrdersNotificationTemplate() throws IOException {
//        ClassPathResource resource = new ClassPathResource(ASIDE_ORDERS_NOTIFICATION_TEMPLATE);
//        try (InputStream is = resource.getInputStream()) {
//            final String text = inputStreamToString(is);
//            final Map<String, String> dataStructure = new HashMap<>();
//            return new Template(ASIDE_ORDERS_NOTIFICATION_TEMPLATE_CODE, text, dataStructure, "Orders waiting");
//        } catch (FileNotFoundException fnfe) {
//            return null;
//        }
//    }

    @Bean(name = TEMPLATES)
    public List<Template> templates() throws IOException {
        List<Template> templates = Lists.newArrayList();
        for(Map.Entry<String, String> templateCodePathEntry : templateCodePathMap.entrySet()) {
            ClassPathResource resource = new ClassPathResource(templateCodePathEntry.getValue());
            try (InputStream is = resource.getInputStream()) {
                final String text = inputStreamToString(is);
                final Map<String, String> dataStructure = new HashMap<>();
                templates.add(new Template(templateCodePathEntry.getKey(), text, dataStructure, "Files not associated to any data storages"));
            } catch (FileNotFoundException fnfe) {
                // due to code construction, it happens and it is not an error or an issue
            }
        }
        return templates;
    }

//    @Bean
//    public Template notSubsettedDataFilesTemplate() throws IOException {
//        ClassPathResource resource = new ClassPathResource(NOT_SUBSETTED_DATA_FILES_TEMPLATE);
//        try (InputStream is = resource.getInputStream()) {
//            final String text = inputStreamToString(is);
//            final Map<String, String> dataStructure = new HashMap<>();
//            return new Template(NOT_SUBSETTED_DATA_FILES_CODE, text, dataStructure, "Files could not be handled by their data storage");
//        } catch (FileNotFoundException fnfe) {
//            return null;
//        }
//    }

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
