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

    private ObjectRandomGenerator root;

    /**
     * Initialize generators and generate the message batch.
     */
    public List<Map<String, Object>> generate(Path templatePath, Integer number) {
        // Initialize generators
        initGenerators(templatePath);
        // Generate messages
        return generate(number);
    }

    /**
     * Initialize generators from specified template. You have to call {@link #generate(Integer)} to generate message with these generators.
     */
    public void initGenerators(Path templatePath) {
        try {
            LOGGER.info("Loading JSON template from {}", templatePath);
            // Load JSON template
            @SuppressWarnings("unchecked")
            Map<String, Object> template = mapper.readValue(templatePath.toFile(), Map.class);
            // Initialize generators
            root = new ObjectRandomGenerator();
            doInitGenerators(template, root);
        } catch (IOException e) {
            String error = String.format("Cannot read json template from path %s", templatePath);
            LOGGER.error(error, e);
            throw new IllegalArgumentException(error);
        }
    }

    @SuppressWarnings("unchecked")
    private void doInitGenerators(Map<String, Object> template, ObjectRandomGenerator generator) {
        for (Entry<String, Object> entry : template.entrySet()) {
            if (Map.class.isAssignableFrom(entry.getValue().getClass())) {
                // Propagate
                ObjectRandomGenerator org = new ObjectRandomGenerator();
                doInitGenerators((Map<String, Object>) entry.getValue(), org);
                generator.addGenerator(entry.getKey(), org);
            } else {
                // Initialize generator
                generator.addGenerator(entry.getKey(), RandomGenerator.of(entry.getValue()));
            }
        }
    }

    /**
     * Generate a message batch based on generators previously initialized calling {@link #initGenerators(Path)}
     */
    public List<Map<String, Object>> generate(Integer number) {
        // Assert generators are ready!
        if (root == null) {
            throw new UnsupportedOperationException(
                    "Generators not ready! Call initGenerators before calling this method!");
        }

        // Generated messages
        List<Map<String, Object>> messages = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            messages.add(root.random());
        }
        return messages;
    }
}
