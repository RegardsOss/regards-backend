/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;

import fr.cnes.regards.modules.templates.domain.Template;

/**
 * @author Xavier-Alexandre Brochard
 * @author Marc Sordi
 * @author oroussel for dismessness
 */
public final class TemplateConfigUtil {

    /**
     * The order created email template as html
     */
    private static final String ORDER_CREATED_TEMPLATE = "template/order-created-template.html";

    /**
     * The aside blablah... (guess the end)
     */
    private static final String ASIDE_ORDERS_NOTIFICATION_TEMPLATE = "template/aside-orders-notification-template.html";

    private TemplateConfigUtil() {}

    public static Template readTemplate(String name, String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream is = resource.getInputStream()) {
            String text = inputStreamToString(is);
            return new Template(name, text);
        }
    }

    /**
     * Writes an {@link InputStream} to a {@link String}.
     * @param is the input stream
     * @return the string
     * @throws IOException when an error occurs while reading the stream
     */
    private static String inputStreamToString(final InputStream is) throws IOException {
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
