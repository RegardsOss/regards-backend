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
package fr.cnes.regards.modules.configuration.service;

import com.google.gson.JsonObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * @param pModuleId
     * @return {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Module retrieveModule(Long pModuleId) throws EntityNotFoundException;

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
     * Retrieve all active modules for the given application Id
     *
     * @param pApplicationId
     * @param pPageable
     * @return Paged list of {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Page<Module> retrieveActiveModules(String pApplicationId, Pageable pPageable);

    /**
     *
     * Save a new module
     *
     * @param pModule
     *            {@link Module} to save
     * @return saved {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Module saveModule(Module pModule) throws EntityInvalidException;

    /**
     *
     * Update a module
     *
     * @param pModule
     *            {@link Module} to update
     * @return updated {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Module updateModule(Module pModule) throws EntityException;

    /**
     *
     * Delete a module
     *
     * @param pModuleId
     *            Module id to delete
     *
     * @since 1.0-SNAPSHOT
     */
    void deleteModule(Long pModuleId) throws EntityNotFoundException;

    /**
     *
     * Add inside the passed module configuration a layer for each dataset
     *
     * @since 3.0-SNAPSHOT
     */
    JsonObject mergeDatasetInsideModuleConf(Module module, JsonObject dataset, String openSearchLink) throws EntityInvalidException;
}
