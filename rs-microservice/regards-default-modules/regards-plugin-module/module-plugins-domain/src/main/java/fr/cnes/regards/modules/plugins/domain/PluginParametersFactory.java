/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.domain;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Utility class to manage a list of plugin parameters.
 *
 * @author cmertz
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
        parameters = new ArrayList<PluginParameter>();
    }

    /**
     *
     * Build a new class
     *
     * @return a factory
     */
    public static PluginParametersFactory build() {
        return new PluginParametersFactory();
    }

    /**
     *
     * Chained set method
     *
     * @param pParameterName
     *            the name parameter
     * 
     * @param pParameterValue
     *            the value parameter
     * 
     * @return the factory
     */
    public PluginParametersFactory addParameter(String pParameterName, String pParameterValue) {
        parameters.add(new PluginParameter(pParameterName, pParameterValue));
        return this;
    }

    /**
     *
     * Chained set method
     *
     * @param pParameterName
     *            the name parameter
     * 
     * @param pParameterValue
     *            the value parameter
     * 
     * @return the factory
     */
    public PluginParametersFactory addParameterDynamic(String pParameterName, String pParameterValue) {
        final PluginParameter aPluginParameter = new PluginParameter(pParameterName, pParameterValue);
        aPluginParameter.setIsDynamic(true);
        parameters.add(aPluginParameter);
        return this;
    }

    /**
     *
     * Chained set method
     *
     * @param pParameterName
     *            the name parameter
     * @param pParameterValue
     *            the value parameter
     * @param pDynamicValues
     *            the set of possible values for the dynamic parameter
     * 
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
     *
     * Chained set method
     *
     * @param pParameterName
     *            the name parameter
     * 
     * @param pPluginConfiguration
     *            the plugin configuration
     * 
     * @return the factory
     */
    public PluginParametersFactory addParameterPluginConfiguration(String pParameterName,
            PluginConfiguration pPluginConfiguration) {
        parameters.add(new PluginParameter(pParameterName, pPluginConfiguration));
        return this;
    }

    public List<PluginParameter> getParameters() {
        return parameters;
    }
}
