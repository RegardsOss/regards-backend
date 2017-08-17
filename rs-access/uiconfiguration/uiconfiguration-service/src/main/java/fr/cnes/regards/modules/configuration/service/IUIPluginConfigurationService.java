/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.configuration.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.modules.configuration.domain.UIPluginConfiguration;
import fr.cnes.regards.modules.configuration.domain.UIPluginDefinition;
import fr.cnes.regards.modules.configuration.domain.UIPluginTypesEnum;

public interface IUIPluginConfigurationService {

    /**
     *
     * Retrieve all plugin configurations
     *
     * @param pIsActive
     *            Return only the active plugins ?. Pass null value to not filter.
     * @param pIsLinkedToAllEntities
     *            Return only the plugins linked to all entities?. Pass null value to not filter.
     * @return Page of {@link UIPluginConfiguration}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginConfiguration> retrievePluginConfigurations(final UIPluginTypesEnum pPluginType, Boolean pIsActive,
            Boolean pIsLinkedToAllEntities, Pageable pPageable);

    /**
     *
     * Retrieve all plugin configurations for the given plugin
     *
     * @param {@link
     *            UIPluginDefinition}'s to search configurations.
     * @param pIsActive
     *            Return only the active plugins ?. Pass null value to not filter.
     * @param pIsLinkedToAllEntities
     *            Return only the plugins linked to all entities?. Pass null value to not filter.
     * @return Page of {@link UIPluginConfiguration}
     * @throws EntityException
     *             throw exception if pPlugin is not defined or not exists.
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginConfiguration> retrievePluginConfigurations(UIPluginDefinition pPlugin, Boolean pIsActive,
            Boolean pIsLinkedToAllEntities, Pageable pPageable) throws EntityException;

    /**
     *
     * Retrieve one plugin configuration.
     *
     * @param pPluginConfigurationId
     *            PluginConfiguration id to retreive
     * @return {@link UIPluginConfiguration}
     * @throws EntityInvalidException
     *             if pPluginConfigurationId is null
     * @since 1.0-SNAPSHOT
     */
    UIPluginConfiguration retrievePluginconfiguration(Long pPluginConfigurationId) throws EntityInvalidException;

    /**
     *
     * Update a plugin configuration.
     *
     * @param UIPluginConfiguration
     *            {@link UIPluginConfiguration} to update
     * @return {@link UIPluginConfiguration}
     * @throws EntityException
     *             if given configuration is null or does not exists
     * @since 1.0-SNAPSHOT
     */
    UIPluginConfiguration updatePluginconfiguration(UIPluginConfiguration pPluginConfiguration) throws EntityException;

    /**
     *
     * Create a plugin configuration.
     *
     * @param UIPluginConfiguration
     *            {@link UIPluginConfiguration} to update
     * @return {@link UIPluginConfiguration}
     * @throws throws
     *             EntityException if pPluginConfiguration already exists
     * @since 1.0-SNAPSHOT
     */
    UIPluginConfiguration createPluginconfiguration(UIPluginConfiguration pPluginConfiguration) throws EntityException;

    /**
     *
     * Delete a plugin configuration.
     *
     * @param UIPluginConfiguration
     *            {@link UIPluginConfiguration} to update
     * @return {@link UIPluginConfiguration}
     * @throws EntityException
     *             if pPluginConfiguration is invalid or does not exists
     * @since 1.0-SNAPSHOT
     */
    void deletePluginconfiguration(UIPluginConfiguration pPluginConfiguration) throws EntityException;

    /**
     *
     * Return all {@link UIPluginConfiguration} for plugins type service and asscociated to the given dataset id.
     *
     * @param pDatasetId
     * @param pServiceScope
     * @return list of {@link UIPluginConfiguration}
     * @since 1.0-SNAPSHOT
     */
    List<UIPluginConfiguration> retrieveActivePluginServices(String pDatasetId);

}
