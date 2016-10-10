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
     * Chainable set method
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

    public List<PluginParameter> getParameters() {
        return parameters;
    }
}
