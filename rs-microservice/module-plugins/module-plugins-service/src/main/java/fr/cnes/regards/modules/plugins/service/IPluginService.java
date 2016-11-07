/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.service;

import java.util.List;

import fr.cnes.regards.modules.plugins.domain.IPluginType;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.plugins.utils.PluginUtilsException;


/**
 *
 * Plugin management service. Manage plugin configuraton, type, instanciation.
 *
 * @author cmertz
 */
public interface IPluginService {

    /**
     *
     * Return all plugin types available
     *
     * @return List<String>
     */
    List<String> getPluginTypes();

    /**
     *
     * Return all <PluginMetaData> available
     *
     * @return
     */
    List<PluginMetaData> getPlugins();

    /**
     *
     * Return all <PluginMetaData> available for a specific plugin type
     *
     * @param pPluginType
     * @return
     */
    List<PluginMetaData> getPluginsByType(String pPluginType);

    /**
     *
     * Get a plugin instance for a given configuration. The pReturnInterfaceType attribute indicates the PluginInterface
     * return type.
     *
     * @param pPluginConfigurationId
     *            a specific plugin's configuration
     * @param pReturnInterfaceType
     *            the plugin's type to return
     * @return a plugin
     * @throws PluginUtilsException
     */
    <T> T getPlugin(Long pPluginConfigurationId, Class<T> pReturnInterfaceType) throws PluginUtilsException;

    /**
     * Get the first plugin instance of a plugin type. The pReturnInterfaceType attribute indicates the PluginInterface
     * return type.
     * 
     * @param pType
     *            the plugin type
     * @param pReturnInterfaceType
     *            the plugin's type to return
     * 
     * @return a plugin
     * @throws PluginUtilsException
     */
    <T> T getFirstPluginByType(IPluginType pType, Class<T> pReturnInterfaceType) throws PluginUtilsException;

    /**
     *
     * Get a specific plugin implementation.
     *
     * @param pPluginImplId
     *            the id of the spscific metadata
     * 
     * @return metadata of a specific plugin's configuration
     */
    PluginMetaData getPluginMetaDataById(String pPluginImplId);

    /**
     *
     * Save a plugin configuration in internal database.
     *
     * @param pPluginConfiguration
     *            the plugin configuration to saved
     * @return the saved plugin configuration
     * @throws PluginUtilsException
     */
    PluginConfiguration savePluginConfiguration(PluginConfiguration pPluginConfiguration) throws PluginUtilsException;

    /**
     *
     * Get the plugin's configuration for a specific configuration
     *
     * @param pId
     * @return a specific configuration
     * @throws PluginUtilsException
     *             TODO CMZ : pourquoi pId
     */
    PluginConfiguration getPluginConfiguration(Long pId) throws PluginUtilsException;

    /**
     *
     * Get all plugin's configuration for a specific plugin type.
     *
     * @param pType
     *            a specific plugin's type
     * @return all the plugin's configuration for a specific plugin type.
     */
    List<PluginConfiguration> getPluginConfigurationsByType(IPluginType pType);

    /**
     *
     * Delete a plugin configuration
     *
     * @param pPluginId
     *            a specific configuration
     * @return
     * @throws PluginUtilsException
     */
    void deletePluginConfiguration(Long pPluginId) throws PluginUtilsException;

    /**
     *
     * Update a plugin configuration
     *
     * @param pPlugin
     *            the plugin's configuration to update
     * @return the updated plugin's configuration
     * @throws RegardsServiceException
     */
    PluginConfiguration updatePluginConfiguration(PluginConfiguration pPlugin) throws PluginUtilsException;
}
