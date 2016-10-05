/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.plugins.utils;

import java.util.List;

import org.omg.PortableInterceptor.RequestInfo;

import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;



/**
 *
 * This class is intended to be used for test purpose only. It uses the core plugin mechanism to instantiate a plugin so
 * the test should be as realistic as possible.
 *
 * @author msordi
 * @since 1.0-SNAPSHOT
 */
public class PluginManagerServiceTest {

    private PluginManagerServiceTest() {
    }

    /**
     *
     * Create an instance of plugin based on its configuration and metadata
     *
     * @param pPluginConf
     *            the plugin configuration
     * @param pPluginMetadata
     *            the plugin metadata
     * @param pReturnInterfaceType
     *            the required returned type
     * @return an instance
     * @throws PluginUtilsException
     * @throws PluginManagerException
     *             if problem occurs
     * @since 1.0-SNAPSHOT
     */
    public static <T> T getPlugin(RequestInfo pRequestInfo, List<PluginParameter> pParameters,
            Class<T> pReturnInterfaceType) throws PluginUtilsException {
        // Build plugin metadata
        final PluginMetaData pluginMetadata = AbstractPluginUtils.createPluginMetaData(pReturnInterfaceType);
        final PluginConfiguration pluginConfiguration = new PluginConfiguration(pluginMetadata, "", pParameters, 0);
        return AbstractPluginUtils.getPlugin(pluginConfiguration, pluginMetadata);
    }
}
