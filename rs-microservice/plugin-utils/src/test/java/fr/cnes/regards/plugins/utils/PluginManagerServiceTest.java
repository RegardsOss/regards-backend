/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.plugins.utils;

import java.util.List;

import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.plugins.utils.AbstractPluginUtils;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 *
 * This class is intended to be used for test purpose only. It uses the core plugin mechanism to instantiate a plugin so
 * the test should be as realistic as possible.
 *
 * @author cmertz
 */
public final class PluginManagerServiceTest {

    /**
     * Default constructor
     */
    private PluginManagerServiceTest() {
    }

    /**
     *
     * Create an instance of plugin based on its configuration and metadata
     *
     * @param <T>
     *            a plugin
     * @param pParameters
     *            the plugin parameters
     * @param pReturnInterfaceType
     *            the required returned type
     * @param pPluginParameters
     *            an optional list of {@link PluginParameter}
     * 
     * @return an instance
     * @throws PluginUtilsException
     *             if problem occurs
     */
    public static <T> T getPlugin(List<PluginParameter> pParameters, Class<T> pReturnInterfaceType,
            PluginParameter... pPluginParameters) throws PluginUtilsException {
        // Build plugin metadata
        final PluginMetaData pluginMetadata = AbstractPluginUtils.createPluginMetaData(pReturnInterfaceType);

        final PluginConfiguration pluginConfiguration = new PluginConfiguration(pluginMetadata, "", pParameters, 0);
        return AbstractPluginUtils.getPlugin(pluginConfiguration, pluginMetadata, pPluginParameters);
    }

    /**
     *
     * Create an instance of plugin configuration
     *
     * @param <T>
     *            a plugin
     * @param pParameters
     *            the plugin parameters
     * @param pReturnInterfaceType
     *            the required returned type
     * 
     * @return an instance
     * @throws PluginUtilsException
     *             if problem occurs
     */
    public static <T> PluginConfiguration getPluginConfiguration(List<PluginParameter> pParameters,
            Class<T> pReturnInterfaceType) throws PluginUtilsException {
        // Build plugin metadata
        final PluginMetaData pluginMetadata = AbstractPluginUtils.createPluginMetaData(pReturnInterfaceType);

        return new PluginConfiguration(pluginMetadata, "", pParameters, 0);
    }

}
