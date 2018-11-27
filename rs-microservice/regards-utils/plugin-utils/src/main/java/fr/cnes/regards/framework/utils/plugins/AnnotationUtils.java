/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils for plugin annotations
 * @author Marc Sordi
 */
public final class AnnotationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationUtils.class);

    /**
     * Markdown extension available for plugin parameter description
     */
    private static final String MARKDOWN_EXTENSION = ".md";

    private static final String END_LINE = "\n";

    private AnnotationUtils() {
        // Static class
    }

    /**
     * This method allows to load a markdown file from jar archives using {@link Class#getResourceAsStream(String)}
     * @param clazz class to document defining the target package
     * @param filename markdown filename
     * @return markdown text as a single string
     */
    public static String loadMarkdown(Class<?> clazz, String filename) {
        if ((filename != null) && !filename.isEmpty() && filename.endsWith(MARKDOWN_EXTENSION)) {
            // Try to load markdown description
            // JDK 8 implementation does not work with ubber jar at the moment
            // try {
            // URL url = pluginClass.getResource(description);
            // if (url != null) {
            // URI uri = url.toURI();
            // Path path = Paths.get(uri);
            // StringBuilder data = new StringBuilder();
            // Stream<String> lines = Files.lines(path);
            // lines.forEach(line -> data.append(line).append(END_LINE));
            // lines.close();
            // return data.toString().trim();
            // }
            // } catch (URISyntaxException | IOException e) {
            // LOGGER.warn("Markdown description cannot be loaded", e);
            // // Fall back to raw description
            // // Nothing to do
            // }
            // Alternative implementation
            try {
                StringBuilder data = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(clazz.getResourceAsStream(filename)))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        data.append(line).append(END_LINE);
                    }
                }
                return data.toString().trim();
            } catch (IOException e) {
                LOGGER.warn("Markdown description cannot be loaded", e);
                // Fall back to raw description
                // Nothing to do
            }
        }
        return filename;
    }

}
