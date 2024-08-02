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
package fr.cnes.regards.modules.storage.rest;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.fileaccess.dto.*;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.service.cache.CacheService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationConfigurationService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Controller to access REST Actions on storage locations.
 *
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(StorageLocationController.BASE_PATH)
public class StorageLocationController implements IResourceController<StorageLocationDto> {

    public static final String BASE_PATH = "/storages";

    public static final String RUN_MONITORING = "/monitoring/run";

    public static final String FILES = "/files";

    public static final String ID_PATH = "/{id}";

    public static final String COPY = "/copy";

    public static final String RETRY = "/retry/{type}";

    public static final String RETRY_SESSION = "/retry/{source}/{session}";

    public static final String UP_PATH = ID_PATH + "/up";

    public static final String DOWN_PATH = ID_PATH + "/down";

    public static final String RUN_PERIODIC_ACTION_PATH = "/periodic-actions/run";

    public static final String RESET_PARAM = "reset";

    public static final String METHOD_DELETE_FILES = "deleteFiles";

    public static final String METHOD_INCREASE_STORAGE_LOCATION_PRIORITY = "increaseStorageLocationPriority";

    public static final String METHOD_DECREASE_STORAGE_LOCATION_PRIORITY = "decreaseStorageLocationPriority";

    public static final String METHOD_UPDATE_LOCATION_CONFIGURATION = "updateLocationConfiguration";

    public static final String METHOD_CONFIGURE_LOCATION = "configureLocation";

    public static final String METHOD_DELETE = "delete";

    public static final String METHOD_COPY_FILES = "copyFiles";

    public static final String METHOD_COPY = "copy";

    public static final String METHOD_DOWN = "down";

    public static final String METHOD_UP = "up";

    private static final String REQUESTS_PATH = "/requests/{type}";

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private StorageLocationConfigurationService storageLocationConfigurationService;

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IAuthenticationResolver authenticationResolver;

    /**
     * End-point to retrieve a storage location by his name
     *
     * @param storageLocation storage location name
     * @return {@link StorageLocationDto}
     * @throws ModuleException if location does not exists
     */
    @PostMapping
    @ResourceAccess(description = "Configure a storage location by his name", role = DefaultRole.ADMIN)
    public ResponseEntity<EntityModel<StorageLocationDto>> configureLocation(@Valid @RequestBody
                                                                             StorageLocationDto storageLocation)
        throws ModuleException {
        if (storageLocation.getName().equals(CacheService.CACHE_NAME)) {
            throw new EntityInvalidException(String.format("Storage location %s is a reserved name.",
                                                           CacheService.CACHE_NAME));
        }
        return new ResponseEntity<>(toResource(storageLocationService.configureLocation(storageLocation)),
                                    HttpStatus.CREATED);
    }

    @PostMapping(path = RUN_PERIODIC_ACTION_PATH)
    @ResourceAccess(description = "Force rung of periodic tasks on storage locations", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> runPeriodicTasks() {
        storageLocationService.runPeriodicTasks();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to update a storage location configuration.
     *
     * @param storageLocation to update
     * @return updated {@link StorageLocationDto}
     * @throws ModuleException if location does not exists
     */
    @PutMapping(path = ID_PATH)
    @ResourceAccess(description = "Update a storage location configuration", role = DefaultRole.ADMIN)
    public ResponseEntity<EntityModel<StorageLocationDto>> updateLocationConfiguration(
        @PathVariable(name = "id") String storageName, @Valid @RequestBody StorageLocationDto storageLocation)
        throws ModuleException {
        return new ResponseEntity<>(toResource(storageLocationService.updateLocationConfiguration(storageName,
                                                                                                  storageLocation)),
                                    HttpStatus.OK);
    }

    /**
     * End-point to retrieve all known storage locations
     *
     * @return {@link StorageLocationDto}s
     */
    @GetMapping
    @Operation(summary = "Get known storage locations", description = "Return a list of known storage locations")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "All known storage locations were retrieved.") })
    @ResourceAccess(description = "Endpoint to retrieve list of all known storage locations.",
                    role = DefaultRole.EXPLOIT)
    public ResponseEntity<List<EntityModel<StorageLocationDto>>> retrieve() throws ModuleException {
        List<StorageLocationDto> storageLocations = new ArrayList<>(storageLocationService.getAllLocations());
        storageLocations.add(cacheService.buildStorageLocation());

        storageLocations.sort(Comparator.comparing(StorageLocationDto::getName));

        return new ResponseEntity<>(toResources(storageLocations), HttpStatus.OK);
    }

    /**
     * End-point to retrieve a Storage location by its name
     *
     * @param storageName storage location name
     */
    @GetMapping(path = ID_PATH)
    @ResourceAccess(description = "Retrieve a storage location by its name", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<StorageLocationDto>> retrieve(@PathVariable(name = "id") String storageName)
        throws ModuleException {
        return new ResponseEntity<>(toResource(storageLocationService.getByName(storageName)), HttpStatus.OK);
    }

    /**
     * End-point to delete a storage location configuration
     *
     * @param storageName storage location name to delete
     * @return Void
     */
    @DeleteMapping(path = ID_PATH)
    @ResourceAccess(description = "Delete storage location", role = DefaultRole.ADMIN)
    public ResponseEntity<Void> delete(@PathVariable(name = "id") String storageName) throws ModuleException {
        storageLocationService.delete(storageName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to delete a storage location configuration
     *
     * @param storageName storage location name to delete
     * @return Void
     */
    @DeleteMapping(path = ID_PATH + REQUESTS_PATH)
    @ResourceAccess(description = "Delete storage requests", role = DefaultRole.ADMIN)
    public ResponseEntity<Void> deleteRequests(@PathVariable(name = "id") String storageName,
                                               @PathVariable(name = "type") FileRequestType type,
                                               @RequestParam(name = "status", required = false)
                                               FileRequestStatus status) throws ModuleException {
        storageLocationService.deleteRequests(storageName, type, Optional.ofNullable(status));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to copy files for a given path of a storage location to an other one
     *
     * @param parameters copy parameters
     * @return Void
     */
    @PostMapping(path = FILES + COPY)
    @ResourceAccess(description = "Copy files for a given path of a storage location to an other one",
                    role = DefaultRole.ADMIN)
    public ResponseEntity<Void> copyFiles(@Valid @RequestBody CopyFilesParametersDto parameters)
        throws ModuleException {
        // assert parameters are not null
        Assert.notNull(parameters, "Copy parameters can not be null");
        Assert.notNull(parameters.getFrom(), "Source copy parameters can not be null");
        Assert.notNull(parameters.getFrom().getStorage(), "Source storage location copy parameters can not be null");
        Assert.notNull(parameters.getFrom().getUrl(), "Source storage url to copy parameters can not be null");
        Assert.notNull(parameters.getTo(), "Destination copy parameters can not be null");
        Assert.notNull(parameters.getTo().getStorage(), "Destination storage location copy parameters can not be null");

        // initialize sessionOwner and session
        // By default, sessionOwner is the user requesting the deletion of the files
        String sessionOwner = authenticationResolver.getUser();
        String session = String.format("Copy files from %s to %s - %s",
                                       parameters.getFrom().getStorage(),
                                       parameters.getTo().getStorage(),
                                       OffsetDateTime.now());
        storageLocationService.copyFiles(parameters.getFrom().getStorage(),
                                         parameters.getFrom().getUrl(),
                                         parameters.getTo().getStorage(),
                                         Optional.ofNullable(parameters.getTo().getUrl()),
                                         parameters.getTypes(),
                                         sessionOwner,
                                         session);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to delete all files referenced in a storage location
     *
     * @param storageName storage location name
     * @param forceDelete If true, files are unreferenced even if the physical files cannot be deleted.
     */
    @DeleteMapping(path = ID_PATH + FILES)
    @ResourceAccess(description = "Delete all files of the storage location", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> deleteFiles(@PathVariable(name = "id") String storageName,
                                            @RequestParam(name = "force", required = false) Boolean forceDelete)
        throws ModuleException {
        // initialize sessionOwner and session
        // By default, sessionOwner is the user requesting the deletion of the files
        String sessionOwner = authenticationResolver.getUser();
        String session = String.format("Delete %s files %s", storageName, OffsetDateTime.now());
        // order deletion of files
        if (forceDelete != null) {
            storageLocationService.deleteFiles(storageName, forceDelete, sessionOwner, session);
        } else {
            storageLocationService.deleteFiles(storageName, false, sessionOwner, session);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to retry all files requests in error state for the given storage location and the given request type
     *
     * @param storageName storage location name
     * @param type        {@link FileRequestType} to retry
     * @return Void
     */
    @GetMapping(path = ID_PATH + FILES + RETRY)
    @ResourceAccess(description = "Retry all files requests in error state for the given storage location and the given request type",
                    role = DefaultRole.ADMIN)
    public ResponseEntity<Void> retryErrors(@PathVariable(name = "id") String storageName,
                                            @PathVariable(name = "type") FileRequestType type) throws ModuleException {
        storageLocationService.retryErrors(storageName, type);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to retry all files requests in error state for a given source and session
     *
     * @param source  name of the source
     * @param session name of the session
     * @return Void
     */
    @GetMapping(path = RETRY_SESSION)
    @ResourceAccess(description = "Retry all files requests in error state for a given source and session",
                    role = DefaultRole.ADMIN)
    public ResponseEntity<Void> retryErrorsBySourceAndSession(@PathVariable(name = "source") String source,
                                                              @PathVariable(name = "session") String session) {
        storageLocationService.retryErrorsBySourceAndSession(source, session);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to increase the priority of a storage location. Priority is used to select a storage location during file retrieving if files are
     * stored on multiple locations.
     *
     * @return Void
     */
    @PutMapping(path = UP_PATH)
    @ResourceAccess(description = "Increase a storage location priority", role = DefaultRole.ADMIN)
    public ResponseEntity<Void> increaseStorageLocationPriority(@PathVariable(name = "id") String storageName)
        throws EntityNotFoundException {
        storageLocationService.increasePriority(storageName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to decrease the priority of a storage location. Priority is used to select a storage location during file retrieving if files are
     * stored on multiple locations.
     *
     * @return Void
     */
    @PutMapping(path = DOWN_PATH)
    @ResourceAccess(description = "Decrease a storage location priority", role = DefaultRole.ADMIN)
    public ResponseEntity<Void> decreaseStorageLocationPriority(@PathVariable(name = "id") String storageName)
        throws EntityNotFoundException {
        storageLocationService.decreasePriority(storageName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(path = RUN_MONITORING)
    @ResourceAccess(description = "Manually run storage location monitoring.", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> runMonitoring(@RequestParam(name = RESET_PARAM, required = false) Boolean reset) {
        if (reset != null) {
            storageLocationService.monitorStorageLocations(reset);
        } else {
            storageLocationService.monitorStorageLocations(false);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<StorageLocationDto> toResource(StorageLocationDto location, Object... extras) {
        EntityModel<StorageLocationDto> resource = EntityModel.of(location);
        if (location == null) {
            return resource;
        }
        if ((location.getName() != null) && location.getName().equals(CacheService.CACHE_NAME)) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    METHOD_DELETE_FILES,
                                    LinkRelation.of(METHOD_DELETE_FILES),
                                    MethodParamFactory.build(String.class, location.getName()),
                                    MethodParamFactory.build(Boolean.class));
            return resource;
        }
        StorageType type = location.getConfiguration() != null ?
            location.getConfiguration().getStorageType() :
            StorageType.OFFLINE;
        if (type != StorageType.OFFLINE) {
            if (!location.getConfiguration().getPriority().equals(StorageLocationConfiguration.HIGHEST_PRIORITY)) {
                resourceService.addLink(resource,
                                        this.getClass(),
                                        METHOD_INCREASE_STORAGE_LOCATION_PRIORITY,
                                        LinkRelation.of(METHOD_UP),
                                        MethodParamFactory.build(String.class, location.getName()));
            }
            if (!location.getConfiguration()
                         .getPriority()
                         .equals(storageLocationConfigurationService.getLowestPriority(location.getConfiguration()
                                                                                               .getStorageType()))) {
                resourceService.addLink(resource,
                                        this.getClass(),
                                        METHOD_DECREASE_STORAGE_LOCATION_PRIORITY,
                                        LinkRelation.of(METHOD_DOWN),
                                        MethodParamFactory.build(String.class, location.getName()));
            }
            resourceService.addLink(resource,
                                    this.getClass(),
                                    METHOD_COPY_FILES,
                                    LinkRelation.of(METHOD_COPY),
                                    MethodParamFactory.build(CopyFilesParametersDto.class));
            resourceService.addLink(resource,
                                    this.getClass(),
                                    METHOD_DELETE_FILES,
                                    LinkRelation.of(METHOD_DELETE_FILES),
                                    MethodParamFactory.build(String.class, location.getName()),
                                    MethodParamFactory.build(Boolean.class));
        }
        // If storage location is configured so delete & edit End-point is also available
        if ((location.getConfiguration() != null)) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    METHOD_UPDATE_LOCATION_CONFIGURATION,
                                    LinkRels.UPDATE,
                                    MethodParamFactory.build(String.class, location.getName()),
                                    MethodParamFactory.build(StorageLocationDto.class));
            resourceService.addLink(resource,
                                    this.getClass(),
                                    METHOD_DELETE,
                                    LinkRels.DELETE,
                                    MethodParamFactory.build(String.class, location.getName()));
        } else {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    METHOD_CONFIGURE_LOCATION,
                                    LinkRels.UPDATE,
                                    MethodParamFactory.build(StorageLocationDto.class));
        }
        return resource;
    }
}
