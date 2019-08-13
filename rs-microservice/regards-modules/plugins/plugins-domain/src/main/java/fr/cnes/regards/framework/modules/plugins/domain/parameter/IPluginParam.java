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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
    PluginParamType getType();

    /**
     * Check if parameter is dynamic (i.e. value depends on the processing context)
     */
    boolean isDynamic();

    /**
     * Check if parameter has dynamic values
     */
    boolean hasDynamicValues();

    /**
     * Remove dynamic values and switch to static parameter
     */
    void toStatic();

    /**
     * Check if dynamic parameter is consistent with static one
     */
    default boolean isValid(IPluginParam dynamicParam) {
        return false;
    }

    /**
     * Check if parameter has a corresponding value
     */
    boolean hasValue();

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

    default void illegalValueForParameter() {
        throw new IllegalArgumentException(
                String.format("Value is not compatible with \"%s\" parameter type", getType()));
    }

    static StringPluginParam build(String name, String value) {
        return new StringPluginParam().with(name, value);
    }

    default void value(String value) {
        if (PluginParamType.STRING.equals(getType())) {
            ((StringPluginParam) this).setValue(value);
        } else {
            illegalValueForParameter();
        }
    }

    static BytePluginParam build(String name, Byte value) {
        return new BytePluginParam().with(name, value);
    }

    default void value(Byte value) {
        if (PluginParamType.BYTE.equals(getType())) {
            ((BytePluginParam) this).setValue(value);
        } else {
            illegalValueForParameter();
        }
    }

    static ShortPluginParam build(String name, Short value) {
        return new ShortPluginParam().with(name, value);
    }

    default void value(Short value) {
        if (PluginParamType.SHORT.equals(getType())) {
            ((ShortPluginParam) this).setValue(value);
        } else {
            illegalValueForParameter();
        }
    }

    static IntegerPluginParam build(String name, Integer value) {
        return new IntegerPluginParam().with(name, value);
    }

    default void value(Integer value) {
        if (PluginParamType.INTEGER.equals(getType())) {
            ((IntegerPluginParam) this).setValue(value);
        } else {
            illegalValueForParameter();
        }
    }

    static LongPluginParam build(String name, Long value) {
        return new LongPluginParam().with(name, value);
    }

    default void value(Long value) {
        if (PluginParamType.LONG.equals(getType())) {
            ((LongPluginParam) this).setValue(value);
        } else {
            illegalValueForParameter();
        }
    }

    static FloatPluginParam build(String name, Float value) {
        return new FloatPluginParam().with(name, value);
    }

    default void value(Float value) {
        if (PluginParamType.FLOAT.equals(getType())) {
            ((FloatPluginParam) this).setValue(value);
        } else {
            illegalValueForParameter();
        }
    }

    static DoublePluginParam build(String name, Double value) {
        return new DoublePluginParam().with(name, value);
    }

    default void value(Double value) {
        if (PluginParamType.DOUBLE.equals(getType())) {
            ((DoublePluginParam) this).setValue(value);
        } else {
            illegalValueForParameter();
        }
    }

    static BooleanPluginParam build(String name, Boolean value) {
        return new BooleanPluginParam().with(name, value);
    }

    default void value(Boolean value) {
        if (PluginParamType.BOOLEAN.equals(getType())) {
            ((BooleanPluginParam) this).setValue(value);
        } else {
            illegalValueForParameter();
        }
    }

    static JsonCollectionPluginParam build(String name, Collection<JsonElement> value) {
        return new JsonCollectionPluginParam().with(name, value);
    }

    default void value(Collection<JsonElement> value) {
        if (PluginParamType.COLLECTION.equals(getType())) {
            ((JsonCollectionPluginParam) this).setValue(value);
        } else {
            illegalValueForParameter();
        }
    }

    static JsonMapPluginParam build(String name, Map<String, JsonElement> value) {
        return new JsonMapPluginParam().with(name, value);
    }

    default void value(Map<String, JsonElement> value) {
        if (PluginParamType.MAP.equals(getType())) {
            ((JsonMapPluginParam) this).setValue(value);
        } else {
            illegalValueForParameter();
        }
    }

    static JsonObjectPluginParam build(String name, JsonObject value) {
        return new JsonObjectPluginParam().with(name, value);
    }

    default void value(JsonObject value) {
        if (PluginParamType.POJO.equals(getType())) {
            ((JsonObjectPluginParam) this).setValue(value);
        } else {
            illegalValueForParameter();
        }
    }

    /**
     * Build a plugin parameter referencing a nested plugin configuration with its business identifier
     * @param name parameter name
     * @key plugin business identifier
     */
    static NestedPluginParam plugin(String name, String identifier) {
        return new NestedPluginParam().with(name, identifier);
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
