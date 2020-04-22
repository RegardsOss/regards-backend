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
package fr.cnes.regards.framework.random.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ObjectRandomGenerator extends AbstractRandomGenerator<Map<String, Object>> {

    private final Map<String, RandomGenerator<?>> generators = new HashMap<>();

    private final Map<String, RandomGenerator<?>> dependentGenerators = new HashMap<>();

    /**
     * Work in progress object
     */
    private Map<String, Object> embedded;

    public ObjectRandomGenerator() {
        super(null);
    }

    @Override
    public Map<String, Object> random() {
        embedded = new HashMap<>();
        for (Entry<String, RandomGenerator<?>> entry : generators.entrySet()) {
            embedded.put(entry.getKey(), entry.getValue().random());
        }
        return embedded;
    }

    @Override
    public Map<String, Object> randomWithContext(Map<String, Object> context) {
        for (Entry<String, RandomGenerator<?>> entry : dependentGenerators.entrySet()) {
            embedded.put(entry.getKey(), entry.getValue().randomWithContext(context));
        }
        // Propagate to embedded objects
        for (Entry<String, RandomGenerator<?>> entry : generators.entrySet()) {
            if (ObjectRandomGenerator.class.isAssignableFrom(entry.getValue().getClass())) {
                entry.getValue().randomWithContext(context);
            }
        }
        return embedded;
    }

    public Map<String, RandomGenerator<?>> getGenerators() {
        return generators;
    }

    public void addGenerator(String key, RandomGenerator<?> generator) {
        if (generator.getDependentProperties().isPresent()) {
            this.dependentGenerators.put(key, generator);
        } else {
            this.generators.put(key, generator);
        }
    }
}
