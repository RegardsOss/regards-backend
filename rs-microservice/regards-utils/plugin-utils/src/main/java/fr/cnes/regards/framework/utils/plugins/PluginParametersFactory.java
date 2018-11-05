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

package fr.cnes.regards.framework.utils.plugins;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameterValue;

/**
 * Utility class to manage a {@link List} of {@link PluginParameter}.
 * @author Christophe Mertz
 * @author Marc Sordi
 */
public class PluginParametersFactory {

    /**
     * List of {@link PluginParameter}
     */
    private final Set<PluginParameter> parameters = Sets.newHashSet();

    /**
     * Constructor
     */
    public PluginParametersFactory() {
    }

    /**
     * Constructor with the {@link List} of {@link PluginParameter}
     * @param parameters the {@link List} of {@link PluginParameter}
     */
    public PluginParametersFactory(Collection<PluginParameter> parameters) {
        this.parameters.addAll(parameters);
    }

    /**
     * Build a new class
     * @return a factory
     */
    public static PluginParametersFactory build() {
        return new PluginParametersFactory();
    }

    /**
     * Build a new class and set the {@link List} of {@link PluginParameter}
     * @param parameters the {@link List} of {@link PluginParameter}
     * @return a factory
     */
    public static PluginParametersFactory build(Set<PluginParameter> parameters) {
        return new PluginParametersFactory(parameters);
    }

    /**
     * Add a parameter
     * @param name the name parameter
     * @param value may be an {@link Object}, a {@link Collection} or a {@link Map}.
     * @return the factory
     */
    public PluginParametersFactory addParameter(String name, Object value) {
        parameters.add(new PluginParameter(name, normalize(value)));
        return this;
    }

    /**
     * Update a parameter value
     * @param parameter parameter to update
     * @param value value to normalize
     */
    public static PluginParameter updateParameter(PluginParameter parameter, Object value) {
        parameter.setValue(normalize(value));
        return parameter;
    }

    /**
     * Update parameter properties
     * @param parameter parameter to update
     * @param value value to normalize
     * @param isDynamic dynamic or not
     * @param dynamicValues dynamic values
     */
    public static <T> void updateParameter(PluginParameter parameter, T value, boolean isDynamic,
            List<T> dynamicValues) {
        parameter.setValue(normalize(value));
        parameter.setIsDynamic(isDynamic);
        // Manage possible dynamic values
        if ((dynamicValues != null) && !dynamicValues.isEmpty()) {
            Set<PluginParameterValue> dyns = new HashSet<>();
            dynamicValues.forEach(s -> dyns.add(PluginParameterValue.create((normalize(s)))));
            parameter.setDynamicsValues(dyns);
        }
    }

    /**
     * Add a dynamic parameter
     * @param name the name parameter
     * @param value may be an {@link Object}, a {@link Collection} or a {@link Map}.
     * @return the factory
     */
    public PluginParametersFactory addDynamicParameter(String name, Object value) {
        PluginParameter parameter = new PluginParameter(name, normalize(value));
        parameter.setIsDynamic(true);
        parameters.add(parameter);
        return this;
    }

    /**
     * Add a parameter which can only be dynamic
     * @param name the name parameter
     * @param value may be an {@link Object}, a {@link Collection} or a {@link Map}.
     * @return the factory
     */
    public PluginParametersFactory addOnlyDynamicParameter(String name, Object value) {
        PluginParameter parameter = new PluginParameter(name, normalize(value));
        parameter.setOnlyDynamic(true);
        parameters.add(parameter);
        return this;
    }

    /**
     * Add a dynamic parameter with its dynamic values
     * @param name the name parameter
     * @param value may be an {@link Object}, a {@link Collection} or a {@link Map}.
     * @return the factory
     */
    public <T> PluginParametersFactory addDynamicParameter(String name, T value, List<T> dynamicValues) {

        PluginParameter parameter = new PluginParameter(name, normalize(value));
        parameter.setIsDynamic(true);
        // Manage possible dynamic values
        if ((dynamicValues != null) && !dynamicValues.isEmpty()) {
            Set<PluginParameterValue> dyns = new HashSet<>();
            dynamicValues.forEach(s -> dyns.add(PluginParameterValue.create((normalize(s)))));
            parameter.setDynamicsValues(dyns);
        }
        parameters.add(parameter);
        return this;
    }

    /**
     * Transform value to {@link String}
     * @param value object
     * @return {@link String} value
     */
    private static String normalize(Object value) {
        return PluginGsonUtils.getInstance().toJson(value);
    }

    /**
     * Chained set method
     * @param name the name parameter
     * @param pluginConfiguration the plugin configuration
     * @return the factory
     */
    public PluginParametersFactory addPluginConfiguration(String name, PluginConfiguration pluginConfiguration) {
        parameters.add(new PluginParameter(name, pluginConfiguration));
        return this;
    }

    /**
     * Remove a {@link PluginParameter} from the {@link List}
     * @param pluginParameter the {@link PluginParameter} to remove
     * @return the factory
     */
    public PluginParametersFactory removeParameter(PluginParameter pluginParameter) {
        parameters.remove(pluginParameter);
        return this;
    }

    /**
     * Get all parameters as list
     * @return {@link PluginParameter} list
     */
    public Set<PluginParameter> getParameters() {
        return parameters;
    }

    /**
     * Get all parameters as array
     * @return {@link PluginParameter} array
     */
    public PluginParameter[] asArray() {
        if (parameters == null) {
            return null;
        }
        return parameters.toArray(new PluginParameter[parameters.size()]);
    }
}
