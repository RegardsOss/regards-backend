/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnes.regards.framework.random.function.IPropertyGetter;
import fr.cnes.regards.framework.random.generator.ObjectRandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Look at markdown for usage
 */
public class Generator {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    private final RandomGeneratorResolver randomGeneratorResolver;

    private ObjectRandomGenerator root;

    protected Generator(RandomGeneratorResolver randomGeneratorResolver,
                        Path templatePath,
                        IPropertyGetter propertyGetter) {
        this.randomGeneratorResolver = randomGeneratorResolver;
        initGenerators(templatePath, propertyGetter);
    }

    /**
     * Initialize generators from specified template. You have to call {@link #generate(Integer)} to generate message with these generators.
     */
    private void initGenerators(Path templatePath, IPropertyGetter propertyGetter) {
        try {
            LOGGER.info("Loading JSON template from {}", templatePath);
            // Load JSON template
            @SuppressWarnings("unchecked") Map<String, Object> template = mapper.readValue(templatePath.toFile(),
                                                                                           Map.class);
            // Initialize generators
            root = new ObjectRandomGenerator();
            doInitGenerators(template, root, propertyGetter);
        } catch (IOException e) {
            String error = String.format("Cannot read json template from path %s", templatePath);
            LOGGER.error(error, e);
            throw new IllegalArgumentException(error);
        }
    }

    @SuppressWarnings("unchecked")
    private void doInitGenerators(Map<String, Object> template,
                                  ObjectRandomGenerator generator,
                                  IPropertyGetter propertyGetter) {
        for (Entry<String, Object> entry : template.entrySet()) {
            if ((entry.getValue() != null) && Map.class.isAssignableFrom(entry.getValue().getClass())) {
                // Propagate
                ObjectRandomGenerator org = new ObjectRandomGenerator();
                LOGGER.trace("Initializing generator for {} with value {}", entry.getKey(), entry.getValue());
                doInitGenerators((Map<String, Object>) entry.getValue(), org, propertyGetter);
                generator.addGenerator(entry.getKey(), org);
            } else {
                // Initialize generator
                generator.addGenerator(entry.getKey(), randomGeneratorResolver.get(entry.getValue(), propertyGetter));
            }
        }
    }

    /**
     * Generate a message batch based on generators previously initialized calling {@link #initGenerators(Path, IPropertyGetter)}
     *
     * @param generatedObjects number of object to generate
     */
    public List<Map<String, Object>> generate(Integer generatedObjects) {
        // Assert generators are ready!
        if (root == null) {
            throw new UnsupportedOperationException(
                "Generators not ready! Call initGenerators before calling this method!");
        }

        // Generated messages
        List<Map<String, Object>> messages = new ArrayList<>();
        for (int i = 0; i < generatedObjects; i++) {
            // First pass generation generates independent values
            // Second pass uses generated values!
            messages.add(root.randomWithContext(root.random()));
        }
        return messages;
    }
}
