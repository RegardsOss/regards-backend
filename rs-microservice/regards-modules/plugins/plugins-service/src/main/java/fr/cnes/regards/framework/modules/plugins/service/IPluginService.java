/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.AbstractPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;

/**
 * Plugin management service.
 * @author Christophe Mertz
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 */
public interface IPluginService {

    /**
     * Return all plugin types detected.
     * @return Set<String>
     */
    Set<String> getPluginTypes();

    /**
     * Return available plugin types i.e. all plugin types which at least one implementation is detected.
     * @return available plugin types i.e. all plugin types which at least one implementation is detected.
     */
    Set<String> getAvailablePluginTypes();

    /**
     * Return all {@link PluginMetaData} available
     * @return list of {@link PluginMetaData}
     */
    List<PluginMetaData> getPlugins();

    /**
     * Return all specific plugin type available {@link PluginMetaData}s.
     * @param interfacePluginType a specific interface plugin type
     * @return list of {@link PluginMetaData}s
     */
    List<PluginMetaData> getPluginsByType(Class<?> interfacePluginType);

    /**
     * @return whether the plugin configured by the given plugin configuration threw its id can be instantiated or not
     * @throws NotAvailablePluginConfigurationException
     * @deprecated use {@link #canInstantiate(String)} instead
     */
    @Deprecated
    boolean canInstantiate(Long id) throws ModuleException, NotAvailablePluginConfigurationException;

    /**
     * Use {@link #canInstantiate(String)} with business identifier to be instance independent.
     * @return whether the plugin configured by the given plugin configuration threw its id can be instantiated or not
     * @throws NotAvailablePluginConfigurationException
     */
    boolean canInstantiate(String businessId) throws ModuleException, NotAvailablePluginConfigurationException;

    /**
     * Use {@link #getPlugin(String, IPluginParam...)} with business identifier to be instance independent.
     * Get a plugin instance for a given configuration. The pReturnInterfaceType attribute indicates the PluginInterface
     * return type.
     * @param <T> a plugin instance
     * @param id database identifier of the {@link PluginConfiguration}.
     * @param dynamicPluginParameters list of dynamic {@link AbstractPluginParam}
     * @return a plugin instance
     * @throws ModuleException thrown if we cannot find any PluginConfiguration corresponding to pId
     * @throws NotAvailablePluginConfigurationException
     * @deprecated use {@link #getPlugin(String, IPluginParam...)} instead
     */
    @Deprecated
    <T> T getPlugin(Long id, IPluginParam... dynamicPluginParameters)
            throws ModuleException, NotAvailablePluginConfigurationException;

    /**
     * Get a plugin instance for a given configuration. The pReturnInterfaceType attribute indicates the PluginInterface
     * return type.
     * @param <T> a plugin instance
     * @param businessId business identifier of the {@link PluginConfiguration}.
     * @param dynamicPluginParameters list of dynamic {@link AbstractPluginParam}
     * @return a plugin instance
     * @throws ModuleException thrown if we cannot find any PluginConfiguration corresponding to pId
     * @throws NotAvailablePluginConfigurationException
     */
    <T> T getPlugin(String businessId, IPluginParam... dynamicPluginParameters)
            throws ModuleException, NotAvailablePluginConfigurationException;

    /**
     * Get the first plugin instance of a plugin type. The pReturnInterfaceType attribute indicates the PluginInterface
     * return type.
     * @param <T> a plugin instance
     * @param interfacePluginType a specific interface plugin type
     * @param pluginParameters an optional list of {@link AbstractPluginParam}
     * @return a plugin instance
     * @throws ModuleException thrown if an error occurs
     * @throws NotAvailablePluginConfigurationException
     */
    <T> T getFirstPluginByType(Class<?> interfacePluginType, IPluginParam... pluginParameters)
            throws ModuleException, NotAvailablePluginConfigurationException;

    /**
     * Get a specific plugin implementation.
     * @param pluginImplId the id of the specific metadata
     * @return a {@link PluginMetaData} for a specific {@link PluginConfiguration}
     */
    PluginMetaData getPluginMetaDataById(String pluginImplId);

    /**
     * Save a {@link PluginConfiguration} in internal database.
     * @param pluginConfiguration the plugin configuration to saved
     * @return the saved {@link PluginConfiguration}
     */
    PluginConfiguration savePluginConfiguration(PluginConfiguration pluginConfiguration)
            throws EntityInvalidException, EncryptionException, EntityNotFoundException;

    /**
     * Delete a {@link PluginConfiguration}.
     * @param businessId business identifier of the {@link PluginConfiguration}.
     * @throws ModuleException Entity to delete does not exist
     */
    void deletePluginConfiguration(String businessId) throws ModuleException;

    /**
     * Update a {@link PluginConfiguration}.
     * @param plugin the {@link PluginConfiguration} to update
     * @return the updated {@link PluginConfiguration}
     * @throws ModuleException plugin to update does not exists
     */
    PluginConfiguration updatePluginConfiguration(PluginConfiguration plugin) throws ModuleException;

    /**
     * Get the {@link PluginConfiguration}.
     * Use {@link #getPluginConfiguration(String)} with business identifier to be instance independent.
     * @param id business identifier of the {@link PluginConfiguration}.
     * @return a specific configuration
     * @throws EntityNotFoundException thrown if we cannot find any PluginConfiguration corresponding to pId
     * @deprecated use {@link #getPluginConfiguration(String)} instead
     */
    @Deprecated
    PluginConfiguration getPluginConfiguration(Long id) throws EntityNotFoundException;

    /**
     * Get the {@link PluginConfiguration}.
     * @param businessId business identifier of the {@link PluginConfiguration}.
     * @return a specific configuration
     * @throws EntityNotFoundException thrown if we cannot find any PluginConfiguration corresponding
     */
    PluginConfiguration getPluginConfiguration(String businessId) throws EntityNotFoundException;

    /**
     * Load a PluginConfiguration with all its relations
     * @param id {@link Long}
     * @return {@link PluginConfiguration}
     */
    PluginConfiguration loadPluginConfiguration(String businessId);

    /**
     * Does given PluginConfiguration exist ?
     * @param businessId business identifier of the {@link PluginConfiguration}.
     * @return true or false (it's a boolean !!!)
     */
    boolean exists(String businessId);

    /**
     * Does given PluginConfiguration exist ?
     * @param pluginConfLabel PluginConfiguration label to test
     * @return true or false (it's a boolean !!!)
     */
    boolean existsByLabel(String pluginConfLabel);

    /**
     * Get all plugin's configuration for a specific plugin type.
     * @param interfacePluginType a specific interface plugin type
     * @return all the {@link PluginConfiguration} for a specific plugin type.
     */
    List<PluginConfiguration> getPluginConfigurationsByType(Class<?> interfacePluginType);

    /**
     * Get all plugin's configuration.
     * @return all the {@link PluginConfiguration}.
     */
    List<PluginConfiguration> getAllPluginConfigurations();

    /**
     * Get all plugin's configuration for a specific plugin Id.
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
     * Get {@link PluginMetaData} for a plugin of a specific plugin type.</br>
     * If the plugin class name does not match a plugin of the plugin type, an exception is thrown.
     * @param clazz the plugin type
     * @param pluginClassName a plugin class name
     * @return the {@link PluginMetaData} of the plugin of plugin type
     * @throws EntityInvalidException No plugin of plugin type found.
     */
    PluginMetaData checkPluginClassName(Class<?> clazz, String pluginClassName) throws EntityInvalidException;

    /**
     * Get a PluginConfiguration according to its unique label
     * @param configurationLabel the configuration label
     * @return the plugin configuration
     */
    PluginConfiguration getPluginConfigurationByLabel(String configurationLabel) throws EntityNotFoundException;

    /**
     * Find an optional PluginConfiguration according to its unique label
     * @param configurationLabel the configuration label
     * @return the plugin configuration
     */
    Optional<PluginConfiguration> findPluginConfigurationByLabel(String configurationLabel);

    /**
     * Check if plugin is cached (resolving tenant internally)
     * @param businessId business identifier
     * @return {@link boolean}
     */
    boolean isPluginCached(String businessId);

    /**
     * Remove plugin instance cache with specified configuration identifier (resolving tenant internally)
     * @param businessId business identifier
     */
    void cleanPluginCache(String businessId);

    /**
     * Remove all plugin instances from cache
     */
    void cleanPluginCache();

    /**
     * @return tenant plugin cache
     */
    Map<String, Object> getPluginCache();

    /**
     * @param businessId business identifier
     * @return tenant plugin instance
     */
    Object getCachedPlugin(String businessId);

    /**
     * Prepare {@link PluginConfiguration} by decrypting all crypted values.
     * A clone is return only if values are mutated! It may be a shallow clone so only use it for configuration export.
     * @param pluginConf {@link PluginConfiguration} to export
     * @return exported {@link PluginConfiguration}
     */
    PluginConfiguration prepareForExport(PluginConfiguration pluginConf);
}
