/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.fileaccess.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.fileaccess.dto.StorageLocationConfigurationDto;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Client to access storage location configuration endpoints on file-access microservice.
 *
 * @author Sébastien Binda
 */
@RestClient(name = "rs-file-access", contextId = "rs-file-access.config.client")
public interface IStorageLocationConfigurationClient {

    // PATHS

    String BASE_PATH = "/storages";

    String ID_PATH = "/{id}";

    // GET ENDPOINTS

    /**
     * Retrieve all storage location configurations.
     */
    @GetMapping(path = BASE_PATH,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Retrieve all storage location configurations.", role = DefaultRole.EXPLOIT)
    ResponseEntity<List<EntityModel<StorageLocationConfigurationDto>>> retrieveAllStorageLocationConfigs();

    /**
     * Retrieve an existing storage location configuration by name
     */
    @GetMapping(path = BASE_PATH + ID_PATH,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Retrieve an existing storage location configuration by name.",
                    role = DefaultRole.EXPLOIT)
    ResponseEntity<EntityModel<StorageLocationConfigurationDto>> retrieveStorageLocationConfigByName(
        @PathVariable(name = "id") String storageName);

    // POST/PUT ENDPOINTS

    /**
     * Create a new storage location configuration.
     *
     * @param storageLocationConfigDto Storage location configuration to create.
     * @throws ModuleException if the configuration could not be created.
     */
    @PostMapping(path = BASE_PATH,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Create a new storage location configuration.", role = DefaultRole.ADMIN)
    ResponseEntity<EntityModel<StorageLocationConfigurationDto>> createStorageLocationConfig(
        @Valid @RequestBody StorageLocationConfigurationDto storageLocationConfigDto) throws ModuleException;

    /**
     * Update an existing storage location configuration by unique name.
     *
     * @param storageName              unique  storage location configuration identifier.
     * @param storageLocationConfigDto Storage location configuration to be updated.
     * @return the updated configuration
     * @throws ModuleException if the configuration could not be updated.
     */
    @PutMapping(path = BASE_PATH + ID_PATH,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Update an existing storage location configuration.", role = DefaultRole.ADMIN)
    ResponseEntity<EntityModel<StorageLocationConfigurationDto>> updateStorageLocationConfigByName(
        @PathVariable(name = "id") String storageName,
        @Valid @RequestBody StorageLocationConfigurationDto storageLocationConfigDto) throws ModuleException;

    // DELETE ENDPOINTS

    /**
     * Delete an existing storage location configuration dto by unique name.
     *
     * @param storageName unique  storage location configuration identifier.
     * @throws ModuleException if the configuration could not be deleted
     */
    @DeleteMapping(BASE_PATH + ID_PATH)
    @ResourceAccess(description = "Delete an existing storage location configuration by name.",
                    role = DefaultRole.ADMIN)
    ResponseEntity<Void> deleteStorageLocationConfigByName(@PathVariable(name = "id") String storageName)
        throws ModuleException;
}
