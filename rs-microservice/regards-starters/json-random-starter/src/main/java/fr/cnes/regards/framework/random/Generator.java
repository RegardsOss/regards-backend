/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.random;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.cnes.regards.framework.random.generator.ObjectRandomGenerator;
import fr.cnes.regards.framework.random.generator.RandomGenerator;

public class Generator {

    private static Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> generate(Path templatePath, Integer number) {
        // Generated messages
        List<Map<String, Object>> messages = new ArrayList<>();

        try {
            LOGGER.info("Loading JSON template from {}", templatePath);
            // Load JSON template
            Map<String, Object> template = mapper.readValue(templatePath.toFile(), Map.class);
            // Initialize generators
            ObjectRandomGenerator root = new ObjectRandomGenerator();
            prepareGenerators(template, root);
            // Generate messages
            for (int i = 0; i < number; i++) {
                messages.add(root.random());
            }
        } catch (IOException e) {
            String error = String.format("Cannot read json template from path %s", templatePath);
            LOGGER.error(error, e);
            throw new IllegalArgumentException(error);
        }

        return messages;
    }

    @SuppressWarnings("unchecked")
    private void prepareGenerators(Map<String, Object> template, ObjectRandomGenerator generator) {
        for (Entry<String, Object> entry : template.entrySet()) {
            if (Map.class.isAssignableFrom(entry.getValue().getClass())) {
                // Propagate
                ObjectRandomGenerator org = new ObjectRandomGenerator();
                prepareGenerators((Map<String, Object>) entry.getValue(), org);
                generator.addGenerator(entry.getKey(), org);
            } else {
                // Initialize generator
                generator.addGenerator(entry.getKey(), RandomGenerator.of(entry.getValue()));
            }
        }
    }
}
