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
package fr.cnes.regards.modules.access.services.service.ui;

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
     * @param pPluginId
     * @return {@link UIPluginDefinition}
     * @since 1.0-SNAPSHOT
     */
    UIPluginDefinition retrievePlugin(Long pPluginId) throws EntityNotFoundException;

    /**
     *
     * Retrieve all plugins
     *
     * @return Paged list of {@link UIPluginDefinition}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginDefinition> retrievePlugins(Pageable pPageable);

    /**
     *
     * Retrieve all plugins for the given type
     *
     * @return Paged list of {@link UIPluginDefinition}
     * @since 1.0-SNAPSHOT
     */
    Page<UIPluginDefinition> retrievePlugins(UIPluginTypesEnum pType, Pageable pPageable);

    /**
     *
     * Save a new plugin
     *
     * @param pTheme
     *            {@link UIPluginDefinition} to save
     * @return saved {@link UIPluginDefinition}
     * @since 1.0-SNAPSHOT
     */
    UIPluginDefinition savePlugin(UIPluginDefinition pPlugin) throws EntityInvalidException;

    /**
     *
     * Update a plugin
     *
     * @param pTheme
     *            {@link UIPluginDefinition} to update
     * @return updated {@link UIPluginDefinition}
     * @since 1.0-SNAPSHOT
     */
    UIPluginDefinition updatePlugin(UIPluginDefinition pPlugin) throws EntityNotFoundException, EntityInvalidException;

    /**
     *
     * Delete a plugin
     *
     * @param pPluginId
     *            {@link UIPluginDefinition} id to delete
     *
     * @since 1.0-SNAPSHOT
     */
    void deletePlugin(Long pPluginId) throws ModuleException;
}
