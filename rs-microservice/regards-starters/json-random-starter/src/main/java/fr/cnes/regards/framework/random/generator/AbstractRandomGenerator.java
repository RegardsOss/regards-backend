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

import java.util.Map;

import fr.cnes.regards.framework.random.function.FunctionDescriptor;

public abstract class AbstractRandomGenerator<T> implements RandomGenerator<T> {

    private static final String JSON_PATH_SEPARATOR = ".";

    protected final FunctionDescriptor fd;

    public AbstractRandomGenerator(FunctionDescriptor fd) {
        this.fd = fd;
    }

    @SuppressWarnings("unchecked")
    protected Object findValue(Map<String, Object> context, String jsonPath) {
        int firstSeparator = jsonPath.indexOf(JSON_PATH_SEPARATOR);
        if (firstSeparator == -1) {
            if (context.containsKey(jsonPath)) {
                return context.get(jsonPath);
            } else {
                throw new IllegalArgumentException(
                        String.format("Key %s does not exist in current context %s", jsonPath, context));
            }
        } else {
            String levelPath = jsonPath.substring(0, firstSeparator);
            String remainingPath = jsonPath.substring(firstSeparator + 1, jsonPath.length());
            Object embedded = context.get(levelPath);
            if (Map.class.isAssignableFrom(embedded.getClass())) {
                return findValue((Map<String, Object>) embedded, remainingPath);
            }
            throw new IllegalArgumentException(
                    String.format("JSON path %s does not match a real path in current context %s", jsonPath, context));
        }
    }
}
