/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.framework.modules.plugins.service;

import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;

/**
 * Plugin management service.
 *
 * @author Christophe Mertz
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 */
public interface IPluginService {

    /**
     * Return all plugin types available.
     *
     * @return List<String>
     */
    List<String> getPluginTypes();

    /**
     * Return all {@link PluginMetaData} available
     *
     * @return list of {@link PluginMetaData}
     */
    List<PluginMetaData> getPlugins();

    /**
     * Return all {@link PluginMetaData} available for a specific plugin type.
     *
     * @param pInterfacePluginType a specific interface plugin type
     * @return list of {@link PluginMetaData}
     */
    List<PluginMetaData> getPluginsByType(Class<?> pInterfacePluginType);

    /**
     * Get a plugin instance for a given configuration. The pReturnInterfaceType attribute indicates the PluginInterface
     * return type.
     *
     * @param <T> a plugin instance
     * @param pPluginConfigurationId the id of a {@link PluginConfiguration}.
     * @param pPluginParameters an optional list of {@link PluginParameter}
     * @return a plugin instance
     * @throws ModuleException thrown if we cannot find any PluginConfiguration corresponding to pId
     */
    <T> T getPlugin(Long pPluginConfigurationId, final PluginParameter... pPluginParameters) throws ModuleException;

    /**
     * Get a plugin instance for a {@link PluginConfiguration}
     *
     * @param <T> a plugin instance
     * @param pPluginConfiguration a {@link PluginConfiguration}.
     * @return a plugin instance
     * @throws ModuleException thrown if we cannot find any PluginConfiguration corresponding to pId
     */
    <T> T getPlugin(PluginConfiguration pPluginConfiguration) throws ModuleException;

    /**
     * Get the first plugin instance of a plugin type. The pReturnInterfaceType attribute indicates the PluginInterface
     * return type.
     *
     * @param <T> a plugin instance
     * @param pInterfacePluginType a specific interface plugin type
     * @param pPluginParameters an optional list of {@link PluginParameter}
     * @return a plugin instance
     * @throws ModuleException thrown if an error occurs
     */
    <T> T getFirstPluginByType(Class<?> pInterfacePluginType, final PluginParameter... pPluginParameters)
            throws ModuleException;

    /**
     * Get a specific plugin implementation.
     *
     * @param pPluginImplId the id of the specific metadata
     * @return a {@link PluginMetaData} for a specific {@link PluginConfiguration}
     */
    PluginMetaData getPluginMetaDataById(String pPluginImplId);

    /**
     * Save a {@link PluginConfiguration} in internal database.
     *
     * @param pPluginConfiguration the plugin configuration to saved
     * @return the saved {@link PluginConfiguration}
     * @throws ModuleException thrown if an error occurs
     */
    PluginConfiguration savePluginConfiguration(PluginConfiguration pPluginConfiguration) throws ModuleException;

    /**
     * Delete a {@link PluginConfiguration}.
     *
     * @param pConfId a specific configuration
     * @return
     * @throws ModuleException Entity to delete does not exist
     */
    void deletePluginConfiguration(Long pConfId) throws ModuleException;

    /**
     * Update a {@link PluginConfiguration}.
     *
     * @param pPlugin the {@link PluginConfiguration} to update
     * @return the updated {@link PluginConfiguration}
     * @throws ModuleException plugin to update does not exists
     */
    PluginConfiguration updatePluginConfiguration(PluginConfiguration pPlugin) throws ModuleException;

    /**
     * Get the {@link PluginConfiguration}.
     *
     * @param pId a plugin identifier
     * @return a specific configuration
     * @throws ModuleException thrown if we cannot find any PluginConfiguration corresponding to pId
     */
    PluginConfiguration getPluginConfiguration(Long pId) throws ModuleException;

    /**
     * Load a PluginConfiguration with all its relations
     */
    PluginConfiguration loadPluginConfiguration(Long id);

    boolean exists(Long pId);

    /**
     * Get all plugin's configuration for a specific plugin type.
     *
     * @param pInterfacePluginType a specific interface plugin type
     * @return all the {@link PluginConfiguration} for a specific plugin type.
     */
    List<PluginConfiguration> getPluginConfigurationsByType(Class<?> pInterfacePluginType);

    /**
     * Get all plugin's configuration.
     *
     * @return all the {@link PluginConfiguration}.
     */
    List<PluginConfiguration> getAllPluginConfigurations();

    /**
     * Get all plugin's configuration for a specific plugin Id.
     *
     * @param pPluginId a specific plugin Id
     * @return all the {@link PluginConfiguration} for a specific plugin Id
     */
    List<PluginConfiguration> getPluginConfigurations(String pPluginId);

    /**
     * Add a package to scan to find the plugins.
     *
     * @param pPluginPackage A package name to scan to find the plugins.
     */
    public void addPluginPackage(String pPluginPackage);

    /**
     * Get {@link PluginMetaData} for a plugin of a specific plugin type.</br>
     * If the plugin class name does not match a plugin of the plugin type, a exception is thrown.
     *
     * @param pClass the plugin type
     * @param pPluginClassName a plugin class name
     * @return the {@link PluginMetaData} of the plugin of plugin type
     * @throws EntityInvalidException Any plugin of plugin type is find.
     */
    public PluginMetaData checkPluginClassName(Class<?> pClass, String pPluginClassName) throws EntityInvalidException;

    /**
     * Get a PluginConfiguration according to its unique label
     *
     * @param pConfigurationLabel the configuration label
     * @return the plugin configuration
     * @throws EntityNotFoundException
     */
    PluginConfiguration getPluginConfigurationByLabel(String pConfigurationLabel) throws EntityNotFoundException;

    /**
     * Remove plugin instance cache with specified configuration identifier
     * @param pConfId plugin configuration identifier
     */
    void cleanPluginCache(Long pConfId);

}
