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

package fr.cnes.regards.framework.modules.plugins.domain;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Utility class to manage a {@link List} of {@link PluginParameter}.
 *
 * @author Christophe Mertz
 */
public class PluginParametersFactory {

    /**
     * List of {@link PluginParameter}
     */
    private List<PluginParameter> parameters;

    /**
     * Constructor
     *
     */
    public PluginParametersFactory() {
        parameters = new ArrayList<>();
    }

    /**
     * Constructor with the {@link List} of {@link PluginParameter}
     * 
     * @param pPluginParameters
     *            the {@link List} of {@link PluginParameter}
     */
    public PluginParametersFactory(List<PluginParameter> pPluginParameters) {
        parameters = pPluginParameters;
    }

    /**
     * Build a new class
     *
     * @return a factory
     */
    public static PluginParametersFactory build() {
        return new PluginParametersFactory();
    }

    /**
     * Build a new class and set the {@link List} of {@link PluginParameter}
     * 
     * @param pPluginParameters
     *            the {@link List} of {@link PluginParameter}
     * @return a factory
     */
    public static PluginParametersFactory build(List<PluginParameter> pPluginParameters) {
        return new PluginParametersFactory(pPluginParameters);
    }

    /**
     * Chained addParameter method
     *
     * @param pParameterName
     *            the name parameter
     * @param pParameterValue
     *            the value parameter
     * @return the factory
     */
    public PluginParametersFactory addParameter(String pParameterName, String pParameterValue) {
        parameters.add(new PluginParameter(pParameterName, pParameterValue));
        return this;
    }

    /**
     * Chained addParameterDynamic method
     *
     * @param pParameterName
     *            the name parameter
     * @param pParameterValue
     *            the value parameter
     * @return the factory
     */
    public PluginParametersFactory addParameterDynamic(String pParameterName, String pParameterValue) {
        final PluginParameter aPluginParameter = new PluginParameter(pParameterName, pParameterValue);
        aPluginParameter.setIsDynamic(true);
        parameters.add(aPluginParameter);
        return this;
    }

    /**
     * Chained addParameterDynamic method
     *
     * @param pParameterName
     *            the name parameter
     * @param pParameterValue
     *            the value parameter
     * @param pDynamicValues
     *            the set of possible values for the dynamic parameter
     * @return the factory
     */
    public PluginParametersFactory addParameterDynamic(String pParameterName, String pParameterValue,
            List<String> pDynamicValues) {
        final List<PluginDynamicValue> dyns = new ArrayList<>();
        final PluginParameter aPluginParameter = new PluginParameter(pParameterName, pParameterValue);
        aPluginParameter.setIsDynamic(true);

        if (pDynamicValues != null && !pDynamicValues.isEmpty()) {
            pDynamicValues.forEach(s -> dyns.add(new PluginDynamicValue(s)));
        }

        aPluginParameter.setDynamicsValues(dyns);
        parameters.add(aPluginParameter);
        return this;
    }

    /**
     * Chained set method
     *
     * @param pParameterName
     *            the name parameter
     * @param pPluginConfiguration
     *            the plugin configuration
     * @return the factory
     */
    public PluginParametersFactory addParameterPluginConfiguration(String pParameterName,
            PluginConfiguration pPluginConfiguration) {
        parameters.add(new PluginParameter(pParameterName, pPluginConfiguration));
        return this;
    }

    /**
     * Remove a {@link PluginParameter} from the {@link List}
     * 
     * @param pPluginParameter
     *            the {@link PluginParameter} to remove
     * @return the factory
     */
    public PluginParametersFactory removeParameter(PluginParameter pPluginParameter) {
        parameters.remove(pPluginParameter);
        return this;
    }

    public List<PluginParameter> getParameters() {
        return parameters;
    }
}
