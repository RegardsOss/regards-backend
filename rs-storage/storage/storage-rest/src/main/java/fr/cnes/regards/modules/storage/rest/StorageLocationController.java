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
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.dto.CopyFilesParametersDTO;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;
import fr.cnes.regards.modules.storage.domain.plugin.StorageType;
import fr.cnes.regards.modules.storage.service.cache.CacheService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationConfigurationService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Controller to access REST Actions on storage locations.
 *
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(StorageLocationController.BASE_PATH)
public class StorageLocationController implements IResourceController<StorageLocationDTO> {

    public static final String BASE_PATH = "/storages";

    public static final String RUN_MONITORING = "/monitoring/run";

    public static final String FILES = "/files";

    public static final String ID_PATH = "/{id}";

    public static final String COPY = "/copy";

    public static final String RETRY = "/retry/{type}";

    public static final String RETRY_SESSION = "/retry/{source}/{session}";

    public static final String UP_PATH = ID_PATH + "/up";

    public static final String DOWN_PATH = ID_PATH + "/down";

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
    private StorageLocationService service;

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
     * @return {@link StorageLocationDTO}
     * @throws ModuleException if location does not exists
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Configure a storage location by his name", role = DefaultRole.ADMIN)
    public ResponseEntity<EntityModel<StorageLocationDTO>> configureLocation(@Valid @RequestBody
                                                                             StorageLocationDTO storageLocation)
        throws ModuleException {
        if (storageLocation.getName().equals(CacheService.CACHE_NAME)) {
            throw new EntityInvalidException(String.format("Storage location %s is a reserved name.",
                                                           CacheService.CACHE_NAME));
        }
        return new ResponseEntity<>(toResource(service.configureLocation(storageLocation)), HttpStatus.CREATED);
    }

    /**
     * End-point to update a storage location configuration.
     *
     * @param storageLocation to update
     * @return updated {@link StorageLocationDTO}
     * @throws ModuleException if location does not exists
     */
    @RequestMapping(method = RequestMethod.PUT, path = ID_PATH)
    @ResourceAccess(description = "Update a storage location configuration", role = DefaultRole.ADMIN)
    public ResponseEntity<EntityModel<StorageLocationDTO>> updateLocationConfiguration(
        @PathVariable(name = "id") String storageId, @Valid @RequestBody StorageLocationDTO storageLocation)
        throws ModuleException {
        return new ResponseEntity<>(toResource(service.updateLocationConfiguration(storageId, storageLocation)),
                                    HttpStatus.OK);
    }

    /**
     * End-point to retrieve all known storage locations
     *
     * @return {@link StorageLocationDTO}s
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve list of all known storage locations", role = DefaultRole.EXPLOIT)
    public ResponseEntity<List<EntityModel<StorageLocationDTO>>> retrieve() throws ModuleException {
        Collection<StorageLocationDTO> allLocations = service.getAllLocations();
        allLocations.add(cacheService.toStorageLocation());
        return new ResponseEntity<>(toResources(allLocations), HttpStatus.OK);
    }

    /**
     * End-point to retrieve a Storage location by his name
     *
     * @param storageId storage location name
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.GET, path = ID_PATH)
    @ResourceAccess(description = "Retrieve list of all known storage locations", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<StorageLocationDTO>> retrieve(@PathVariable(name = "id") String storageId)
        throws ModuleException {
        return new ResponseEntity<>(toResource(service.getById(storageId)), HttpStatus.OK);
    }

    /**
     * End-point to delete a storage location configuration
     *
     * @param storageLocationId storage location name to delete
     * @return Void
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.DELETE, path = ID_PATH)
    @ResourceAccess(description = "Delete storage location", role = DefaultRole.ADMIN)
    public ResponseEntity<Void> delete(@PathVariable(name = "id") String storageLocationId) throws ModuleException {
        service.delete(storageLocationId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to delete a storage location configuration
     *
     * @param storageLocationId storage location name to delete
     * @return Void
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.DELETE, path = ID_PATH + REQUESTS_PATH)
    @ResourceAccess(description = "Delete storage requests", role = DefaultRole.ADMIN)
    public ResponseEntity<Void> deleteRequests(@PathVariable(name = "id") String storageLocationId,
                                               @PathVariable(name = "type") FileRequestType type,
                                               @RequestParam(name = "status", required = false)
                                               FileRequestStatus status) throws ModuleException {
        service.deleteRequests(storageLocationId, type, Optional.ofNullable(status));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to delete all files referenced in a storage location
     *
     * @param storageLocationId storage location name
     * @param forceDelete       If true, files are unreferenced even if the physical files cannot be deleted.
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.DELETE, path = ID_PATH + FILES)
    @ResourceAccess(description = "Delete all files of the storage location", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> deleteFiles(@PathVariable(name = "id") String storageLocationId,
                                            @RequestParam(name = "force", required = false) Boolean forceDelete)
        throws ModuleException {
        // initialize sessionOwner and session
        // By default, sessionOwner is the user requesting the deletion of the files
        String sessionOwner = authenticationResolver.getUser();
        String session = String.format("Delete %s files %s", storageLocationId, OffsetDateTime.now().toString());
        // order deletion of files
        if (forceDelete != null) {
            service.deleteFiles(storageLocationId, forceDelete, sessionOwner, session);
        } else {
            service.deleteFiles(storageLocationId, false, sessionOwner, session);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to copy files for a given path of a storage location to an other one
     *
     * @param parameters copy parameters
     * @return Void
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.POST, path = FILES + COPY)
    @ResourceAccess(description = "Copy files for a given path of a storage location to an other one",
        role = DefaultRole.ADMIN)
    public ResponseEntity<Void> copyFiles(@Valid @RequestBody CopyFilesParametersDTO parameters)
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
        service.copyFiles(parameters.getFrom().getStorage(),
                          parameters.getFrom().getUrl(),
                          parameters.getTo().getStorage(),
                          Optional.ofNullable(parameters.getTo().getUrl()),
                          parameters.getTypes(),
                          sessionOwner,
                          session);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to retry all files requests in error state for the given storage location and the given request type
     *
     * @param storageLocationId storage location name
     * @param type              {@link FileRequestType} to retry
     * @return Void
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.GET, path = ID_PATH + FILES + RETRY)
    @ResourceAccess(
        description = "Retry all files requests in error state for the given storage location and the given request type",
        role = DefaultRole.ADMIN)
    public ResponseEntity<Void> retryErrors(@PathVariable(name = "id") String storageLocationId,
                                            @PathVariable(name = "type") FileRequestType type) throws ModuleException {
        service.retryErrors(storageLocationId, type);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to retry all files requests in error state for a given source and session
     *
     * @param source  name of the source
     * @param session name of the session
     * @return Void
     */
    @RequestMapping(method = RequestMethod.GET, path = RETRY_SESSION)
    @ResourceAccess(description = "Retry all files requests in error state for a given source and session",
        role = DefaultRole.ADMIN)
    public ResponseEntity<Void> retryErrorsBySourceAndSession(@PathVariable(name = "source") String source,
                                                              @PathVariable(name = "session") String session) {
        service.retryErrorsBySourceAndSession(source, session);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to increase the priority of a storage location. Priority is used to select a storage location during file retrieving if files are
     * stored on multiple locations.
     *
     * @param storageLocationId
     * @return Void
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.PUT, path = UP_PATH)
    @ResourceAccess(description = "Increase a storage location priority", role = DefaultRole.ADMIN)
    public ResponseEntity<Void> increaseStorageLocationPriority(@PathVariable(name = "id") String storageLocationId)
        throws EntityNotFoundException {
        service.increasePriority(storageLocationId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to decrease the priority of a storage location. Priority is used to select a storage location during file retrieving if files are
     * stored on multiple locations.
     *
     * @param storageLocationId
     * @return Void
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.PUT, path = DOWN_PATH)
    @ResourceAccess(description = "Decrease a storage location priority", role = DefaultRole.ADMIN)
    public ResponseEntity<Void> decreaseStorageLocationPriority(@PathVariable(name = "id") String storageLocationId)
        throws EntityNotFoundException {
        service.decreasePriority(storageLocationId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, path = RUN_MONITORING)
    @ResourceAccess(description = "Manually run storage location monitoring.", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> runMonitoring(@RequestParam(name = RESET_PARAM, required = false) Boolean reset)
        throws EntityNotFoundException {
        if (reset != null) {
            service.monitorStorageLocations(reset);
        } else {
            service.monitorStorageLocations(false);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<StorageLocationDTO> toResource(StorageLocationDTO location, Object... extras) {
        EntityModel<StorageLocationDTO> resource = EntityModel.of(location);
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
                                    MethodParamFactory.build(CopyFilesParametersDTO.class));
            resourceService.addLink(resource,
                                    this.getClass(),
                                    METHOD_DELETE_FILES,
                                    LinkRelation.of(METHOD_DELETE_FILES),
                                    MethodParamFactory.build(String.class, location.getName()),
                                    MethodParamFactory.build(Boolean.class));
        }
        // If storage location is configured so delete & edit End-point is also available
        if ((location.getConfiguration() != null) && (location.getConfiguration().getId() != null)) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    METHOD_UPDATE_LOCATION_CONFIGURATION,
                                    LinkRels.UPDATE,
                                    MethodParamFactory.build(String.class, location.getName()),
                                    MethodParamFactory.build(StorageLocationDTO.class));
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
                                    MethodParamFactory.build(StorageLocationDTO.class));
        }
        return resource;
    }
}
