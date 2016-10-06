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
 * @author msordi
 */
public class PluginParametersFactory {

    /**
     * List of {@link PluginParameter}
     */
    private List<PluginParameter> parameters_;

    /**
     * Constructor
     *
     */
    public PluginParametersFactory() {
        parameters_ = new ArrayList<PluginParameter>();
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
        parameters_.add(new PluginParameter(pParameterName, pParameterValue));
        return this;
    }

    /**
     *
     * Get method
     *
     * @return list of {@link PluginParameter}
     */
    public List<PluginParameter> getParameters() {
        return parameters_;
    }
}
