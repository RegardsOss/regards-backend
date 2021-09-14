/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RandomGenerator<T> {

    /**
     * Step for parsing generator parameters from function descriptor
     */
    void parseParameters();

    /**
     * If the method return {@link Optional#empty()}, generator will call {@link #random()} method otherwise {@link #randomWithContext(Map)}.
     * @return list of dependent property paths the generator depends on.
     */
    default Optional<List<String>> getDependentProperties() {
        return Optional.empty();
    }

    /**
     * Main function to generate a random value according to template specification as a FIRST PASS generation.
     */
    default T random() {
        throw new UnsupportedOperationException("Random must be override for independent property generation");
    }

    /**
     * Main function to generate a random value according to template specification and depending on another one as a SECOND PASS generation.
     */
    default T randomWithContext(Map<String, Object> context) {
        throw new UnsupportedOperationException(
                "Random with context must be overridden for dependent property generation");
    }
}
