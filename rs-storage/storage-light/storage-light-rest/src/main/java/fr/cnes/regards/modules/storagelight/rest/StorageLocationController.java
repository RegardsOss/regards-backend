/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.rest;

import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storagelight.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.dto.CopyFilesParametersDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;
import fr.cnes.regards.modules.storagelight.service.location.StorageLocationConfigurationService;
import fr.cnes.regards.modules.storagelight.service.location.StorageLocationService;

/**
 * Controller to access REST Actions on storage locations.
 *
 * @author Sébastien Binda
 *
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

    public static final String UP_PATH = ID_PATH + "/up";

    public static final String DOWN_PATH = ID_PATH + "/down";

    public static final String RESET_PARAM = "reset";

    private static final String REQUESTS_PATH = "requests/{type}";

    @Autowired
    private StorageLocationService service;

    @Autowired
    private StorageLocationConfigurationService storageLocationConfigurationService;

    @Autowired
    private IResourceService resourceService;

    /**
     * End-point to retrieve a storage location by his name
     *
     * @param storageLocation storage location name
     * @return {@link StorageLocationDTO}
     * @throws ModuleException if location does not exists
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Configure a storage location by his name", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<StorageLocationDTO>> configureLocation(
            @Valid @RequestBody StorageLocationDTO storageLocation) throws ModuleException {
        return new ResponseEntity<>(toResource(service.configureLocation(storageLocation)), HttpStatus.CREATED);
    }

    /**
     * End-point to update a storage location configuration.
     * @param storageLocation to update
     * @return updated {@link StorageLocationDTO}
     * @throws ModuleException  if location does not exists
     */
    @RequestMapping(method = RequestMethod.PUT, path = ID_PATH)
    @ResourceAccess(description = "Update a storage location configuration", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<StorageLocationDTO>> updateLocationConfiguration(
            @PathVariable(name = "id") String storageId, @Valid @RequestBody StorageLocationDTO storageLocation)
            throws ModuleException {
        return new ResponseEntity<>(toResource(service.updateLocationConfiguration(storageId, storageLocation)),
                HttpStatus.OK);
    }

    /**
     * End-point to retrieve all known storage locations
     * @return {@link StorageLocationDTO}s
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve list of all known storage locations", role = DefaultRole.ADMIN)
    public ResponseEntity<List<Resource<StorageLocationDTO>>> retrieve() throws ModuleException {
        return new ResponseEntity<>(toResources(service.getAllLocations()), HttpStatus.OK);
    }

    /**
     * End-point to retrieve a Storage location by his name
     * @param storageId storage location name
     * @return
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.GET, path = ID_PATH)
    @ResourceAccess(description = "Retrieve list of all known storage locations", role = DefaultRole.ADMIN)
    public ResponseEntity<Resource<StorageLocationDTO>> retrieve(@PathVariable(name = "id") String storageId)
            throws ModuleException {
        return new ResponseEntity<>(toResource(service.getById(storageId)), HttpStatus.OK);
    }

    /**
     * End-point to delete a storage location configuration
     * @param storageLocationId storage location name to delete
     * @return Void
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.DELETE, path = ID_PATH)
    @ResourceAccess(description = "Delete storage location", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> delete(@PathVariable(name = "id") String storageLocationId) throws ModuleException {
        service.delete(storageLocationId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to delete a storage location configuration
     * @param storageLocationId storage location name to delete
     * @return Void
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.DELETE, path = ID_PATH + REQUESTS_PATH)
    @ResourceAccess(description = "Delete storage location", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> deleteRequests(@PathVariable(name = "id") String storageLocationId,
            @PathVariable(name = "type") FileRequestType type) throws ModuleException {
        service.deleteRequests(storageLocationId, type, Optional.of(FileRequestStatus.ERROR));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to delete all files referenced in a storage location
     * @param storageLocationId storage location name
     * @param forceDelete If true, files are unreferenced even if the physical files cannot be deleted.
     * @return
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.DELETE, path = ID_PATH + FILES)
    @ResourceAccess(description = "Delete all files of the storage location", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> deleteFiles(@PathVariable(name = "id") String storageLocationId,
            @RequestParam(name = "force", required = false) Boolean forceDelete) throws ModuleException {
        if (forceDelete != null) {
            service.deleteFiles(storageLocationId, forceDelete);
        } else {
            service.deleteFiles(storageLocationId, false);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to copy files for a given path of a storage location to an other one
     * @param storageLocationId source storage location name
     * @param pathToCopy path on the source storage location to copy
     * @param destinationStorageId destination storage location name
     * @return Void
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.POST, path = FILES + COPY)
    @ResourceAccess(description = "Copy files for a given path of a storage location to an other one",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> copyFiles(@Valid @RequestBody CopyFilesParametersDTO parameters)
            throws ModuleException {
        Assert.notNull(parameters, "Copy parameters can not be null");
        Assert.notNull(parameters.getFrom(), "Source copy parameters can not be null");
        Assert.notNull(parameters.getFrom().getStorage(), "Source storage location copy parameters can not be null");
        Assert.notNull(parameters.getFrom().getUrl(), "Source storage url to copy parameters can not be null");
        Assert.notNull(parameters.getTo(), "Destination copy parameters can not be null");
        Assert.notNull(parameters.getTo().getStorage(), "Destination storage location copy parameters can not be null");
        service.copyFiles(parameters.getFrom().getStorage(), parameters.getFrom().getUrl(),
                          parameters.getTo().getStorage(), Optional.ofNullable(parameters.getTo().getUrl()));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to retry all files requests in error state for the given storage location and the given request type
     * @param storageLocationId storage location name
     * @param type {@link FileRequestType} to retry
     * @return Void
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.GET, path = ID_PATH + FILES + RETRY)
    @ResourceAccess(
            description = "Retry all files requests in error state for the given storage location and the given request type",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> retryErrors(@PathVariable(name = "id") String storageLocationId,
            @PathVariable(name = "type") FileRequestType type) throws ModuleException {
        service.retryErrors(storageLocationId, type);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to increase the priority of a storage location. Priority is used to select a storage location during file retrieving if files are
     * stored on multiple locations.
     * @param storageLocationId
     * @return Void
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.PUT, path = UP_PATH)
    @ResourceAccess(description = "Increase a storage location priority", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> increaseStorageLocationPriority(@PathVariable(name = "id") String storageLocationId)
            throws EntityNotFoundException {
        service.increasePriority(storageLocationId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to decrease the priority of a storage location. Priority is used to select a storage location during file retrieving if files are
     * stored on multiple locations.
     * @param storageLocationId
     * @return Void
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.PUT, path = DOWN_PATH)
    @ResourceAccess(description = "Decrease a storage location priority", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> decreaseStorageLocationPriority(@PathVariable(name = "id") String storageLocationId)
            throws EntityNotFoundException {
        service.decreasePriority(storageLocationId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, path = RUN_MONITORING)
    @ResourceAccess(description = "Manually run storage location monitoring.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> runMonitoring(@RequestParam(name = RESET_PARAM) Boolean reset)
            throws EntityNotFoundException {
        service.monitorStorageLocations(reset);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<StorageLocationDTO> toResource(StorageLocationDTO location, Object... extras) {
        Resource<StorageLocationDTO> resource = new Resource<>(location);
        StorageType type = location.getConfiguration() != null ? location.getConfiguration().getStorageType()
                : StorageType.OFFLINE;
        if (type != StorageType.OFFLINE) {
            if (!location.getConfiguration().getPriority().equals(StorageLocationConfiguration.HIGHEST_PRIORITY)) {
                resourceService.addLink(resource, this.getClass(), "increaseStorageLocationPriority", "up",
                                        MethodParamFactory.build(String.class, location.getName()));
            }
            if (!location.getConfiguration().getPriority().equals(storageLocationConfigurationService
                    .getLowestPriority(location.getConfiguration().getStorageType()))) {
                resourceService.addLink(resource, this.getClass(), "decreaseStorageLocationPriority", "down",
                                        MethodParamFactory.build(String.class, location.getName()));
            }
            resourceService.addLink(resource, this.getClass(), "copyFiles", "copy",
                                    MethodParamFactory.build(CopyFilesParametersDTO.class));
        }
        // Delete files End-point is always available.
        resourceService.addLink(resource, this.getClass(), "deleteFiles", "deleteFiles",
                                MethodParamFactory.build(String.class, location.getName()),
                                MethodParamFactory.build(Boolean.class));
        // If storage location is configured so delete & edit End-point is also available
        if (location.getConfiguration().getId() != null) {
            resourceService.addLink(resource, this.getClass(), "updateLocationConfiguration", LinkRels.UPDATE,
                                    MethodParamFactory.build(String.class, location.getName()),
                                    MethodParamFactory.build(StorageLocationDTO.class));
            resourceService.addLink(resource, this.getClass(), "delete", LinkRels.DELETE,
                                    MethodParamFactory.build(String.class, location.getName()));
        } else {
            resourceService.addLink(resource, this.getClass(), "configureLocation", LinkRels.CREATE,
                                    MethodParamFactory.build(StorageLocationDTO.class));
        }
        return resource;
    }
}
