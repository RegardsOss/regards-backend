/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.domain;

import java.util.List;

import org.omg.PortableInterceptor.RequestInfo;

import fr.cnes.regards.modules.plugins.utils.PluginUtils;
import fr.cnes.regards.modules.plugins.utils.PluginUtilsException;

/**
 *
 * This class is intended to be used for test purpose only. It uses the core plugin mechanism to instantiate a plugin so
 * the test should be as realistic as possible.
 *
 * @author msordi
 */
public final class PluginManagerServiceTest {

    /**
     * 
     */
    private PluginManagerServiceTest() {
    }

    /**
     *
     * Create an instance of plugin based on its configuration and metadata
     *
     * @param pRequestInfo
     *            the request
     * @param pParameters
     *            the plugin's parameters
     * @param pReturnInterfaceType
     *            the required returned type
     * @return an instance
     * @throws PluginUtilsException
     */
    public static <T> T getPlugin(RequestInfo pRequestInfo, List<PluginParameter> pParameters,
            Class<T> pReturnInterfaceType) throws PluginUtilsException {
        // Build plugin metadata
        final PluginMetaData pluginMetadata = PluginUtils.createPluginMetaData(pReturnInterfaceType);
        final PluginConfiguration pluginConfiguration = new PluginConfiguration(pluginMetadata, "", pParameters, 0,
                "pRequestInfo.getProject()");
        return PluginUtils.getPlugin(pluginConfiguration, pluginMetadata);
    }
}
