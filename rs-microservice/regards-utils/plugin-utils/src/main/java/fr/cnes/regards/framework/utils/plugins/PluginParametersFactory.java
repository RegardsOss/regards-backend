/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginDynamicValue;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;

/**
 * Utility class to manage a {@link List} of {@link PluginParameter}.
 * @author Christophe Mertz
 * @author Marc Sordi
 */
public class PluginParametersFactory {

    /**
     * List of {@link PluginParameter}
     */
    private final List<PluginParameter> parameters;

    /**
     * Constructor
     */
    public PluginParametersFactory() {
        parameters = new ArrayList<>();
    }

    /**
     * Constructor with the {@link List} of {@link PluginParameter}
     * @param parameters the {@link List} of {@link PluginParameter}
     */
    public PluginParametersFactory(List<PluginParameter> parameters) {
        this.parameters = parameters;
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
    public static PluginParametersFactory build(List<PluginParameter> parameters) {
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
     * Add a dynamic parameter with its dynamic values
     * @param name the name parameter
     * @param value may be an {@link Object}, a {@link Collection} or a {@link Map}.
     * @return the factory
     */
    public PluginParametersFactory addDynamicParameter(String name, Object value, List<?> dynamicValues) {

        PluginParameter parameter = new PluginParameter(name, normalize(value));
        parameter.setIsDynamic(true);
        // Manage possible dynamic values
        if ((dynamicValues != null) && !dynamicValues.isEmpty()) {
            Set<PluginDynamicValue> dyns = new HashSet<>();
            dynamicValues.forEach(s -> dyns.add(new PluginDynamicValue(normalize(s))));
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
    private String normalize(Object value) {
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

    public List<PluginParameter> getParameters() {
        return parameters;
    }
}
