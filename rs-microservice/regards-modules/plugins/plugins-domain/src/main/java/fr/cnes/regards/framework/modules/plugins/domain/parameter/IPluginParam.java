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
package fr.cnes.regards.framework.modules.plugins.domain.parameter;

import java.util.Collection;
import java.util.Map;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;

/**
 * @author Marc SORDI
 *
 */
public interface IPluginParam {

    /**
     * Check if parameter has a real value set
     */
    boolean hasValue();

    /**
     * Check if parameter value is instance of the specified class
     */
    boolean isInstance(Class<?> clazz);

    static StringPluginParam build(String name, String value) {
        return new StringPluginParam().with(name, value);
    }

    static BytePluginParam build(String name, Byte value) {
        return new BytePluginParam().with(name, value);
    }

    static ShortPluginParam build(String name, Short value) {
        return new ShortPluginParam().with(name, value);
    }

    static IntegerPluginParam build(String name, Integer value) {
        return new IntegerPluginParam().with(name, value);
    }

    static LongPluginParam build(String name, Long value) {
        return new LongPluginParam().with(name, value);
    }

    static FloatPluginParam build(String name, Float value) {
        return new FloatPluginParam().with(name, value);
    }

    static DoublePluginParam build(String name, Double value) {
        return new DoublePluginParam().with(name, value);
    }

    static BooleanPluginParam build(String name, Boolean value) {
        return new BooleanPluginParam().with(name, value);
    }

    static CollectionPluginParam build(String name, Collection<?> value) {
        return new CollectionPluginParam().with(name, value);
    }

    static MapPluginParam build(String name, Map<?, ?> value) {
        return new MapPluginParam().with(name, value);
    }

    static ObjectPluginParam build(String name, Object value) {
        return new ObjectPluginParam().with(name, value);
    }

    static NestedPluginParam build(String name, PluginConfiguration value) {
        return new NestedPluginParam().with(name, value);
    }
}
