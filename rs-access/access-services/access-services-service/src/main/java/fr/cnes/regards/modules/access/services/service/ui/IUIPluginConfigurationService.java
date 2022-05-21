/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.service.ui;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IUIPluginConfigurationService {

    /**
     * Retrieve all plugin configurations
     *
     * @param pluginType
     * @param isActive              Return only the active plugins ?. Pass null value to not filter.
     * @param isLinkedToAllEntities Return only the plugins linked to all entities?. Pass null value to not filter.
     * @param pageable
     * @return Page of {@link UIPluginConfiguration}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginConfiguration> retrievePluginConfigurations(final UIPluginTypesEnum pluginType,
                                                             Boolean isActive,
                                                             Boolean isLinkedToAllEntities,
                                                             Pageable pageable);

    /**
     * Retrieve all plugin configurations for the given plugin
     *
     * @param plugin                {@link UIPluginDefinition}'s to search configurations.
     * @param isActive              Return only the active plugins ?. Pass null value to not filter.
     * @param isLinkedToAllEntities Return only the plugins linked to all entities?. Pass null value to not filter.
     * @param pageable
     * @return Page of {@link UIPluginConfiguration}
     * @throws EntityException throw exception if pPlugin is not defined or not exists.
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginConfiguration> retrievePluginConfigurations(UIPluginDefinition plugin,
                                                             Boolean isActive,
                                                             Boolean isLinkedToAllEntities,
                                                             Pageable pageable) throws EntityException;

    /**
     * Retrieve one plugin configuration.
     *
     * @param pluginConfigurationId PluginConfiguration id to retreive
     * @return {@link UIPluginConfiguration}
     * @throws EntityInvalidException if pPluginConfigurationId is null
     * @since 1.0-SNAPSHOT
     */
    UIPluginConfiguration retrievePluginconfiguration(Long pluginConfigurationId) throws EntityInvalidException;

    /**
     * Update a plugin configuration.
     *
     * @param pluginConfiguration {@link UIPluginConfiguration} to update
     * @return {@link UIPluginConfiguration}
     * @throws EntityException if given configuration is null or does not exists
     * @since 1.0-SNAPSHOT
     */
    UIPluginConfiguration updatePluginconfiguration(UIPluginConfiguration pluginConfiguration) throws EntityException;

    /**
     * Create a plugin configuration.
     *
     * @param pluginConfiguration {@link UIPluginConfiguration} to update
     * @return {@link UIPluginConfiguration}
     * @throws EntityException
     * @throws throws          EntityException if pPluginConfiguration already exists
     * @since 1.0-SNAPSHOT
     */
    UIPluginConfiguration createPluginconfiguration(UIPluginConfiguration pluginConfiguration) throws EntityException;

    /**
     * Delete a plugin configuration.
     *
     * @param pluginConfiguration {@link UIPluginConfiguration} to update
     * @throws EntityException if pPluginConfiguration is invalid or does not exists
     * @since 1.0-SNAPSHOT
     */
    void deletePluginconfiguration(UIPluginConfiguration pluginConfiguration) throws EntityException;

    /**
     * Return all {@link UIPluginConfiguration} for plugins type service and associated to the given dataset id if any given.
     *
     * @param datasetId        Can be <code>null</code>.
     * @param applicationModes Can be <code>null</code>.
     * @return list of {@link UIPluginConfiguration}
     * @since 1.0-SNAPSHOT
     */
    List<UIPluginConfiguration> retrieveActivePluginServices(String datasetId, List<ServiceScope> applicationModes);

    /**
     * Return all {@link UIPluginConfiguration} for plugins type service and associated to the given dataset id if any given.
     *
     * @param pDatasetIds      Can be <code>null</code>.
     * @param applicationModes Can be <code>null</code>.
     * @return list of {@link UIPluginConfiguration}
     * @since 1.0-SNAPSHOT
     */
    List<UIPluginConfiguration> retrieveActivePluginServices(List<String> pDatasetIds,
                                                             List<ServiceScope> applicationModes);

    Page<UIPluginConfiguration> retrievePluginConfigurations(PageRequest pageable);

}
