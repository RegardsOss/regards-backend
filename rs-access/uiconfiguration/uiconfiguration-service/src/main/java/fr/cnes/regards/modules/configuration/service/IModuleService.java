/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.google.gson.JsonObject;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.domain.Module;

/**
 *
 * Class IModuleService
 *
 * Interface for Module service
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RegardsTransactional
public interface IModuleService {

    /**
     *
     * Retreive a module by is id.
     *
     * @param moduleId
     * @return {@link Module}
     * @throws EntityNotFoundException
     * @since 1.0-SNAPSHOT
     */
    Module retrieveModule(Long moduleId) throws EntityNotFoundException;

    /**
     *
     * Retrieve all modules for the given application Id
     *
     * @param applicationId
     * @param active search for active modules
     * @param type module type
     * @param pPageable
     * @return Paged list of {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Page<Module> retrieveModules(String applicationId, Boolean active, String type, Pageable pPageable);

    /**
    *
    * Retrieve all modules for the given application Id
    *
    * @param applicationId
    * @param pageable
    * @return Paged list of {@link Module}
    * @since 1.0-SNAPSHOT
    */
    Page<Module> retrieveModules(Pageable pageable);

    /**
     *
     * Retrieve all active modules for the given application Id
     *
     * @param applicationId
     * @param pageable
     * @return Paged list of {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Page<Module> retrieveActiveModules(String applicationId, Pageable pageable);

    /**
     *
     * Save a new module
     *
     * @param module
     *            {@link Module} to save
     * @return saved {@link Module}
     * @throws EntityInvalidException
     * @since 1.0-SNAPSHOT
     */
    Module saveModule(Module module) throws EntityInvalidException;

    /**
     *
     * Update a module
     *
     * @param module
     *            {@link Module} to update
     * @return updated {@link Module}
     * @throws EntityException
     * @since 1.0-SNAPSHOT
     */
    Module updateModule(Module module) throws EntityException;

    /**
     *
     * Delete a module
     *
     * @param moduleId
     *            Module id to delete
     * @throws EntityNotFoundException
     *
     * @since 1.0-SNAPSHOT
     */
    void deleteModule(Long moduleId) throws EntityNotFoundException;

    /**
     *
     * Add inside the passed module configuration a layer for each dataset
     * @param module
     * @param dataset
     * @param openSearchLink
     * @return {@link JsonObject}
     * @throws EntityInvalidException
     *
     * @since 3.0-SNAPSHOT
     */
    JsonObject addDatasetLayersInsideModuleConf(Module module, JsonObject dataset, String openSearchLink)
            throws EntityInvalidException;
}
