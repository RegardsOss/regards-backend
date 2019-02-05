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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Plugin parameter interface
 *
 * @author Marc SORDI
 *
 */
public interface IPluginParam {

    /**
     * Get parameter name
     */
    String getName();

    /**
     * Get parameter type
     */
    String getType();

    /**
     * Check if parameter is dynamic (i.e. value depends on the processing context)
     */
    boolean isDynamic();

    /**
     * Check if dynamic parameter is consistent with static one
     */
    default boolean isValid(IPluginParam staticParam) {
        return false;
    }

    /**
     * Check if parameter has a corresponding value
     */
    boolean hasValue();

    /**
     * Check if parameter value is instance of the specified class
     */
    boolean isInstance(Class<?> clazz);

    /**
     * Get parameter value
     */
    Object getValue();

    /**
     * Check if parameter supports default value injection. If not, {@link #applyDefaultValue(String)} will throw an {@link UnsupportedOperationException}.
     */
    default boolean supportsDefaultValue() {
        return false;
    }

    /**
     * Apply default value to current parameter
     */
    default void applyDefaultValue(String value) {
        throw new UnsupportedOperationException(
                String.format("Cannot apply default value for parameter \"%s\" of type \"%s\"", getName(), getType()));
    }

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

    /**
     * Build a plugin parameter referencing a nested plugin configuration
     */
    static NestedPluginParam nested(String name, Long configurationId) {
        return new NestedPluginParam().with(name, configurationId);
    }

    static Set<IPluginParam> set(IPluginParam... params) {
        Set<IPluginParam> set = new HashSet<>();
        if (params != null) {
            for (IPluginParam param : params) {
                set.add(param);
            }
        }
        return set;
    }
}
