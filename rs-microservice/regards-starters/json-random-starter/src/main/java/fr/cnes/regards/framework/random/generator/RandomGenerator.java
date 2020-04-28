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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import fr.cnes.regards.framework.random.function.FunctionDescriptor;
import fr.cnes.regards.framework.random.function.FunctionDescriptorParser;

public interface RandomGenerator<T> {

    default void parseParameters() {
        // Step to parse generator parameters from function descriptor if any
    }

    /**
     * If the method return {@link Optional#empty()}, generator will call {@link #random()} method else {@link #randomWithContext(Map)}.
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

    static RandomGenerator<?> of(Object value) {
        // Parse function
        FunctionDescriptor fd = FunctionDescriptorParser.parse(value);
        if (fd == null) {
            return new NoopGenerator(value);
        }

        // Get random generator
        RandomGenerator<?> rg;
        switch (fd.getType()) {
            case BOOLEAN:
                rg = new RandomBoolean(fd);
                break;
            case OFFSET_DATE_TIME:
                rg = new RandomOffsetDateTime(fd);
                break;
            case DOUBLE:
                rg = new RandomDouble(fd);
                break;
            case ENUM:
                rg = new RandomEnum(fd);
                break;
            case FLOAT:
                rg = new RandomFloat(fd);
                break;
            case INTEGER:
                rg = new RandomInteger(fd);
                break;
            case LONG:
                rg = new RandomLong(fd);
                break;
            case NOW:
                rg = new NowGenerator(fd);
                break;
            case SEQUENCE:
                rg = new SequenceGenerator(fd);
                break;
            case STRING:
                rg = new RandomString(fd);
                break;
            case URN:
                rg = new RandomUrn(fd);
                break;
            case UUID:
                rg = new RandomUuid(fd);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported function %s", fd.getType()));
        }
        rg.parseParameters();
        return rg;
    }
}
