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

import fr.cnes.regards.framework.random.function.FunctionDescriptor;
import fr.cnes.regards.framework.random.function.FunctionDescriptorParser;

public interface RandomGenerator<T> {

    T random();

    static RandomGenerator<?> of(Object value) {
        // Parse function
        FunctionDescriptor fd = FunctionDescriptorParser.parse(value);
        if (fd == null) {
            return new NoGenerator(value);
        }

        // Get random generator
        RandomGenerator<?> rg;
        switch (fd.getType()) {
            case BOOLEAN:
                rg = new RandomBoolean(fd);
                break;
            case LOCAL_DATE_TIME:
                rg = new RandomLocalDateTime(fd);
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
            case STRING:
                rg = new RandomString(fd);
                break;
            case UUID:
                rg = new RandomUuid(fd);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported function %s", fd.getType()));
        }
        return rg;
    }
}
