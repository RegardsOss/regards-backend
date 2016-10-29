/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.service;

import java.util.List;

import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 *
 * Plugin management service.
 *
 * @author cmertz
 */
public interface IPluginService {

    /**
     *
     * Return all plugin types available.
     *
     * @return List<String>
     */
    List<String> getPluginTypes();

    /**
     *
     * Return all {@link PluginMetaData} available
     *
     * @return list of {@link PluginMetaData}
     */
    List<PluginMetaData> getPlugins();

    /**
     *
     * Return all {@link PluginMetaData} available for a specific plugin type.
     *
     * @param pInterfacePluginType
     *            a specific interface plugin type
     * @return list of {@link PluginMetaData}
     */
    List<PluginMetaData> getPluginsByType(Class<?> pInterfacePluginType);

    /**
     *
     * Get a plugin instance for a given configuration. The pReturnInterfaceType attribute indicates the PluginInterface
     * return type.
     *
     * @param <T>
     *            a plugin instance
     * @param pPluginConfigurationId
     *            the id of a {@link PluginConfiguration}.
     * @param pPluginParameters
     *            an optional list of {@link PluginParameter}
     * @return a plugin
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    <T> T getPlugin(Long pPluginConfigurationId, final PluginParameter... pPluginParameters)
            throws PluginUtilsException;

    /**
     * Get the first plugin instance of a plugin type. The pReturnInterfaceType attribute indicates the PluginInterface
     * return type.
     * 
     * @param <T>
     *            a plugin instance
     * @param pInterfacePluginType
     *            a specific interface plugin type
     * @param pPluginParameters
     *            an optional list of {@link PluginParameter}
     * @return a plugin
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    <T> T getFirstPluginByType(Class<?> pInterfacePluginType, final PluginParameter... pPluginParameters)
            throws PluginUtilsException;

    /**
     *
     * Get a specific plugin implementation.
     *
     * @param pPluginImplId
     *            the id of the specific metadata
     * @return a {@link PluginMetaData} for a specific {@link PluginConfiguration}
     */
    PluginMetaData getPluginMetaDataById(String pPluginImplId);

    /**
     *
     * Save a {@link PluginConfiguration} in internal database.
     *
     * @param pPluginConfiguration
     *            the plugin configuration to saved
     * @return the saved {@link PluginConfiguration}
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    PluginConfiguration savePluginConfiguration(PluginConfiguration pPluginConfiguration) throws PluginUtilsException;

    /**
     *
     * Delete a {@link PluginConfiguration}.
     *
     * @param pPluginId
     *            a specific configuration
     * @return
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    void deletePluginConfiguration(Long pPluginId) throws PluginUtilsException;

    /**
     *
     * Update a {@link PluginConfiguration}.
     *
     * @param pPlugin
     *            the {@link PluginConfiguration} to update
     * @return the updated {@link PluginConfiguration}
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    PluginConfiguration updatePluginConfiguration(PluginConfiguration pPlugin) throws PluginUtilsException;

    /**
     *
     * Get the {@link PluginConfiguration}.
     *
     * @param pId
     *            a plugin identifier
     * @return a specific configuration
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    PluginConfiguration getPluginConfiguration(Long pId) throws PluginUtilsException;

    /**
     *
     * Get all plugin's configuration for a specific plugin type.
     *
     * @param pInterfacePluginType
     *            a specific interface plugin type
     * @return all the {@link PluginConfiguration} for a specific plugin type.
     */
    List<PluginConfiguration> getPluginConfigurationsByType(Class<?> pInterfacePluginType);

    /**
     *
     * Get all plugin's configuration for a specific plugin Id.
     *
     * @param pPluginId
     *            a specific plugin Id
     * @return all the {@link PluginConfiguration} for a specific plugin Id
     */
    List<PluginConfiguration> getPluginConfigurationsByType(String pPluginId);

}
