/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.plugins.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
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
     * Return all plugin types detected.
     *
     * @return Set<String>
     */
    Set<String> getPluginTypes();

    /**
     * Return available plugin types i.e. all plugin types which at least one implementation is detected.
     *
     * @return available plugin types i.e. all plugin types which at least one implementation is detected.
     */
    Set<String> getAvailablePluginTypes();

    /**
     * Return all {@link PluginMetaData} available
     *
     * @return list of {@link PluginMetaData}
     */
    List<PluginMetaData> getPlugins();

    /**
     * Return all specific plugin type available {@link PluginMetaData}s.
     *
     * @param interfacePluginType a specific interface plugin type
     * @return list of {@link PluginMetaData}s
     */
    List<PluginMetaData> getPluginsByType(Class<?> interfacePluginType);

    /**
     * @param pluginConfigurationId
     * @return whether the plugin configured by the given plugin configuration threw its id cna be instantiated or not
     * @throws ModuleException
     */
    boolean canInstantiate(Long pluginConfigurationId) throws ModuleException;

    /**
     * Get a plugin instance for a given configuration. The pReturnInterfaceType attribute indicates the PluginInterface
     * return type.
     *
     * @param <T> a plugin instance
     * @param pluginConfigurationId the id of a {@link PluginConfiguration}.
     * @param dynamicPluginParameters list of dynamic {@link PluginParameter}
     * @return a plugin instance
     * @throws ModuleException thrown if we cannot find any PluginConfiguration corresponding to pId
     */
    <T> T getPlugin(Long pluginConfigurationId, final PluginParameter... dynamicPluginParameters)
            throws ModuleException;

    /**
     * Get a plugin instance for a {@link PluginConfiguration} and dynamic plugin parameters<br/>
     *
     * Note : this method is just a proxy for {@link IPluginService#getPlugin(Long, PluginParameter...)}
     * so plugin configuration is reloaded from database before instanciation.
     *
     * @deprecated Use {@link IPluginService#getPlugin(Long, PluginParameter...)} instead.
     *
     * @param <T> a plugin instance
     * @param pluginConfiguration a {@link PluginConfiguration}.
     * @param dynamicPluginParameters list of dynamic {@link PluginParameter}
     * @return a plugin instance
     * @throws ModuleException thrown if we cannot find any PluginConfiguration corresponding to pId
     */
    @Deprecated
    default <T> T getPlugin(PluginConfiguration pluginConfiguration, final PluginParameter... dynamicPluginParameters)
            throws ModuleException {
        return getPlugin(pluginConfiguration.getId(), dynamicPluginParameters);
    }

    /**
     * Get the first plugin instance of a plugin type. The pReturnInterfaceType attribute indicates the PluginInterface
     * return type.
     *
     * @param <T> a plugin instance
     * @param interfacePluginType a specific interface plugin type
     * @param pluginParameters an optional list of {@link PluginParameter}
     * @return a plugin instance
     * @throws ModuleException thrown if an error occurs
     */
    <T> T getFirstPluginByType(Class<?> interfacePluginType, final PluginParameter... pluginParameters)
            throws ModuleException;

    /**
     * Get a specific plugin implementation.
     *
     * @param pluginImplId the id of the specific metadata
     * @return a {@link PluginMetaData} for a specific {@link PluginConfiguration}
     */
    PluginMetaData getPluginMetaDataById(String pluginImplId);

    /**
     * Save a {@link PluginConfiguration} in internal database.
     *
     * @param pluginConfiguration the plugin configuration to saved
     * @return the saved {@link PluginConfiguration}
     * @throws EntityInvalidException
     * @throws EncryptionException
     * @throws EntityNotFoundException
     */
    PluginConfiguration savePluginConfiguration(PluginConfiguration pluginConfiguration)
            throws EntityInvalidException, EncryptionException, EntityNotFoundException;

    /**
     * Delete a {@link PluginConfiguration}.
     *
     * @param confId a specific configuration
     * @throws ModuleException Entity to delete does not exist
     */
    void deletePluginConfiguration(Long confId) throws ModuleException;

    /**
     * Update a {@link PluginConfiguration}.
     *
     * @param plugin the {@link PluginConfiguration} to update
     * @return the updated {@link PluginConfiguration}
     * @throws ModuleException plugin to update does not exists
     */
    PluginConfiguration updatePluginConfiguration(PluginConfiguration plugin) throws ModuleException;

    /**
     * Get the {@link PluginConfiguration}.
     *
     * @param id a plugin identifier
     * @return a specific configuration
     * @throws EntityNotFoundException thrown if we cannot find any PluginConfiguration corresponding to pId
     */
    PluginConfiguration getPluginConfiguration(Long id) throws EntityNotFoundException;

    /**
     * Load a PluginConfiguration with all its relations
     * @param id {@link Long}
     * @return  {@link PluginConfiguration}
     */
    PluginConfiguration loadPluginConfiguration(Long id);

    /**
     * Does given PluginConfiguration exist ?
     * @param id PluginConfiguration id to test
     * @return true or false (it's a boolean !!!)
     */
    boolean exists(Long id);

    /**
     * Does given PluginConfiguration exist ?
     * @param pluginConfLabel PluginConfiguration label to test
     * @return true or false (it's a boolean !!!)
     */
    boolean existsByLabel(String pluginConfLabel);

    /**
     * Get all plugin's configuration for a specific plugin type.
     *
     * @param interfacePluginType a specific interface plugin type
     * @return all the {@link PluginConfiguration} for a specific plugin type.
     */
    List<PluginConfiguration> getPluginConfigurationsByType(Class<?> interfacePluginType);

    /**
     * Get all plugin's configuration.
     *
     * @return all the {@link PluginConfiguration}.
     */
    List<PluginConfiguration> getAllPluginConfigurations();

    /**
     * Get all plugin's configuration for a specific plugin Id.
     *
     * @param pluginId a specific plugin Id
     * @return all the {@link PluginConfiguration} for a specific plugin Id
     */
    List<PluginConfiguration> getPluginConfigurations(String pluginId);

    /**
     * Get all active plugin's configurations for a specific plugin Id.
     * @param pluginId a specific plugin Id
     * @return all the active {@link PluginConfiguration} for a specific plugin Id
     */
    List<PluginConfiguration> getActivePluginConfigurations(final String pluginId);

    /**
     * This method is no longer used. Package(s) to scan is define once in properties
     * <code>regards.plugins.packages-to-scan</code>. If not define, default package is used (i.e.
     * <code>fr.cnes.regards</code>)
     * Add a package to scan to find the plugins.
     *
     * @param pluginPackage A package name to scan to find the plugins.
     */
    @Deprecated
    default public void addPluginPackage(String pluginPackage) {
        // Nothing to do
    }

    /**
     * Get {@link PluginMetaData} for a plugin of a specific plugin type.</br>
     * If the plugin class name does not match a plugin of the plugin type, an exception is thrown.
     *
     * @param clazz the plugin type
     * @param pluginClassName a plugin class name
     * @return the {@link PluginMetaData} of the plugin of plugin type
     * @throws EntityInvalidException No plugin of plugin type found.
     */
    PluginMetaData checkPluginClassName(Class<?> clazz, String pluginClassName) throws EntityInvalidException;

    /**
     * Get a PluginConfiguration according to its unique label
     *
     * @param configurationLabel the configuration label
     * @return the plugin configuration
     * @throws EntityNotFoundException
     */
    PluginConfiguration getPluginConfigurationByLabel(String configurationLabel) throws EntityNotFoundException;

    /**
     * Find an optional PluginConfiguration according to its unique label
     *
     * @param configurationLabel the configuration label
     * @return the plugin configuration
     */
    Optional<PluginConfiguration> findPluginConfigurationByLabel(String configurationLabel);

    /**
     * Add plugin instance to cache (resolving tenant internally)
     * @param confId configuration identifier
     * @param plugin plugin instance corresponding to the configuration
     */
    void addPluginToCache(Long confId, Object plugin);

    /**
     * Check if plugin is cached (resolving tenant internally)
     * @param confId configuration identifier
     * @return {@link boolean}
     */
    boolean isPluginCached(Long confId);

    /**
     * Remove plugin instance cache with specified configuration identifier (resolving tenant internally)
     * @param confId configuration identifier
     */
    void cleanPluginCache(Long confId);

    /**
     * Remove all plugin instances from cache
     */
    void cleanPluginCache();

    /**
     * @return tenant plugin cache
     */
    Map<Long, Object> getPluginCache();

    /**
     *
     * @param confId configuration identifier
     * @return tenant plugin instance
     */
    Object getCachedPlugin(Long confId);

    /**
     * Return a {@link PluginConfiguration} for a plugin identifier.
     * If it does not exists, the {@link PluginConfiguration} is created.
     *
     * @param pluginId a pluginidentifier
     * @param interfacePluginType the {@link PluginInterface}
     * @return a {@link PluginConfiguration}
     * @throws ModuleException
     *             an error is trhown
     */
    PluginConfiguration getPluginConfiguration(String pluginId, Class<?> interfacePluginType) throws ModuleException;

    /**
     * Export {@link PluginConfiguration} by removing all internal identifier and decrypting all crypted values.
     * @param pluginConf {@link PluginConfiguration} to export
     * @return exported {@link PluginConfiguration}
     */
    PluginConfiguration exportConfiguration(PluginConfiguration pluginConf);
}
