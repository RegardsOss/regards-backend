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
package fr.cnes.regards.modules.access.services.service.ui;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;

@RegardsTransactional
public interface IUIPluginDefinitionService {

    /**
     *
     * Retreive a Plugin by is id.
     *
     * @param pluginId
     * @return {@link UIPluginDefinition}
     * @throws EntityNotFoundException
     * @since 1.0-SNAPSHOT
     */
    UIPluginDefinition retrievePlugin(Long pluginId) throws EntityNotFoundException;

    /**
     *
     * Retrieve all plugins
     * @param pageable
     *
     * @return Paged list of {@link UIPluginDefinition}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginDefinition> retrievePlugins(Pageable pageable);

    /**
     *
     * Retrieve all plugins for the given type
     * @param type
     * @param pageable
     *
     * @return Paged list of {@link UIPluginDefinition}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginDefinition> retrievePlugins(UIPluginTypesEnum type, Pageable pageable);

    /**
     *
     * Save a new plugin
     * @param plugin
     *
     * @return saved {@link UIPluginDefinition}
     * @throws EntityInvalidException
     * @since 1.0-SNAPSHOT
     */
    UIPluginDefinition savePlugin(UIPluginDefinition plugin) throws EntityInvalidException;

    /**
     *
     * Update a plugin
     * @param plugin
     *
     * @return updated {@link UIPluginDefinition}
     * @throws EntityNotFoundException
     * @throws EntityInvalidException
     * @since 1.0-SNAPSHOT
     */
    UIPluginDefinition updatePlugin(UIPluginDefinition plugin) throws EntityNotFoundException, EntityInvalidException;

    /**
     *
     * Delete a plugin
     *
     * @param pluginId
     *            {@link UIPluginDefinition} id to delete
     * @throws ModuleException
     *
     * @since 1.0-SNAPSHOT
     */
    void deletePlugin(Long pluginId) throws ModuleException;

    /**
     * Retrieve a plugin definition by its name
     * @param name
     * @return {@link UIPluginDefinition}
     */
    Optional<UIPluginDefinition> retrievePlugin(String name);
}
