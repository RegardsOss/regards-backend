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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storagelight.domain.database.StorageLocationConfiguration;
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

    public static final String FILES = "/files";

    public static final String ID_PATH = "/{id}";

    public static final String COPY = "/copy";

    public static final String RETRY = "retry/{type}";

    public static final String UP_PATH = ID_PATH + "/up";

    public static final String DOWN_PATH = ID_PATH + "/down";

    public static final String PATH_COPY_PARAM = "pathToCopy";

    public static final String COPY_LOCATION_DEST_PARAM = "destination";

    @Autowired
    private StorageLocationService service;

    @Autowired
    private StorageLocationConfigurationService prioriterizedStorageService;

    @Autowired
    private IResourceService resourceService;

    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Retrieve list of all known storage locations", role = DefaultRole.ADMIN)
    public ResponseEntity<Resource<StorageLocationDTO>> configureLocation(StorageLocationDTO storageLocation)
            throws ModuleException {
        return new ResponseEntity<>(toResource(service.configureLocation(storageLocation)), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, path = ID_PATH)
    @ResourceAccess(description = "Retrieve list of all known storage locations", role = DefaultRole.ADMIN)
    public ResponseEntity<Resource<StorageLocationDTO>> updateLocationConfiguration(StorageLocationDTO storageLocation)
            throws ModuleException {
        return new ResponseEntity<>(toResource(service.updateLocationConfiguration(storageLocation)), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve list of all known storage locations", role = DefaultRole.ADMIN)
    public ResponseEntity<List<Resource<StorageLocationDTO>>> retrieve() throws ModuleException {
        return new ResponseEntity<>(toResources(service.getAllLocations()), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, path = ID_PATH)
    @ResourceAccess(description = "Retrieve list of all known storage locations", role = DefaultRole.ADMIN)
    public ResponseEntity<Resource<StorageLocationDTO>> retrieve(@PathVariable(name = "id") String storageId)
            throws ModuleException {
        return new ResponseEntity<>(toResource(service.getById(storageId)), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, path = ID_PATH)
    @ResourceAccess(description = "Delete storage location", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> delete(@PathVariable(name = "id") String storageLocationId) throws ModuleException {
        service.delete(storageLocationId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

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

    @RequestMapping(method = RequestMethod.GET, path = ID_PATH + FILES + COPY)
    @ResourceAccess(description = "Copy files for a given path of a storage location to an other one",
            role = DefaultRole.ADMIN)
    public ResponseEntity<Void> copyFiles(@PathVariable(name = "id") String storageLocationId,
            @RequestParam(name = PATH_COPY_PARAM) String pathToCopy,
            @RequestParam(name = COPY_LOCATION_DEST_PARAM) String destinationStorageId) throws ModuleException {
        service.copyFiles(storageLocationId, destinationStorageId, pathToCopy);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, path = ID_PATH + FILES + RETRY)
    @ResourceAccess(
            description = "Retry all files requests in error state for the given storage location and the given request type",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> retryErrors(@PathVariable(name = "id") String storageLocationId,
            @PathVariable(name = "type") FileRequestType type) throws ModuleException {
        service.retryErrors(storageLocationId, type);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, path = UP_PATH)
    @ResourceAccess(description = "increase a data storage priority")
    public ResponseEntity<Void> increaseStorageLocationPriority(@PathVariable(name = "id") String storageLocationId)
            throws EntityNotFoundException {
        service.increasePriority(storageLocationId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, path = DOWN_PATH)
    @ResourceAccess(description = "decrease a data storage priority")
    public ResponseEntity<Void> decreaseStorageLocationPriority(@PathVariable(name = "id") String storageLocationId)
            throws EntityNotFoundException {
        service.decreasePriority(storageLocationId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<StorageLocationDTO> toResource(StorageLocationDTO location, Object... extras) {
        Resource<StorageLocationDTO> resource = new Resource<>(location);
        if (location.getConfiguration() != null) {
            switch (location.getType()) {
                case NEALINE:
                    return toResourceNearline(location, extras);
                case ONLINE:
                    return toResourceOnline(location, extras);
                case OFFLINE:
                    return toResourceOffline(location, extras);
                default:
                    break;
            }
        }
        return resource;
    }

    private Resource<StorageLocationDTO> toResourceNearline(StorageLocationDTO location, Object... extras) {
        Resource<StorageLocationDTO> resource = new Resource<>(location);
        if (!location.getConfiguration().getPriority().equals(StorageLocationConfiguration.HIGHEST_PRIORITY)) {
            resourceService.addLink(resource, this.getClass(), "increaseStorageLocationPriority", "up",
                                    MethodParamFactory.build(String.class, location.getName()));
        }
        if (!location.getConfiguration().getPriority()
                .equals(prioriterizedStorageService.getLowestPriority(StorageType.NEARLINE))) {
            resourceService.addLink(resource, this.getClass(), "decreaseStorageLocationPriority", "down",
                                    MethodParamFactory.build(String.class, location.getName()));
        }
        resourceService.addLink(resource, this.getClass(), "copyFiles", "copy",
                                MethodParamFactory.build(String.class, location.getName()));
        resourceService.addLink(resource, this.getClass(), "deleteFiles", "deleteFiles",
                                MethodParamFactory.build(String.class, location.getName()));
        resourceService.addLink(resource, this.getClass(), "delete", "delete",
                                MethodParamFactory.build(String.class, location.getName()));
        return resource;
    }

    private Resource<StorageLocationDTO> toResourceOnline(StorageLocationDTO location, Object... extras) {
        Resource<StorageLocationDTO> resource = new Resource<>(location);
        if (!location.getConfiguration().getPriority().equals(StorageLocationConfiguration.HIGHEST_PRIORITY)) {
            resourceService.addLink(resource, this.getClass(), "increaseStorageLocationPriority", "up",
                                    MethodParamFactory.build(String.class, location.getName()));
        }
        if (!location.getConfiguration().getPriority()
                .equals(prioriterizedStorageService.getLowestPriority(StorageType.ONLINE))) {
            resourceService.addLink(resource, this.getClass(), "decreaseStorageLocationPriority", "down",
                                    MethodParamFactory.build(String.class, location.getName()));
        }
        resourceService.addLink(resource, this.getClass(), "copyFiles", "copy",
                                MethodParamFactory.build(String.class, location.getName()));
        resourceService.addLink(resource, this.getClass(), "deleteFiles", "deleteFiles",
                                MethodParamFactory.build(String.class, location.getName()));
        resourceService.addLink(resource, this.getClass(), "delete", "delete",
                                MethodParamFactory.build(String.class, location.getName()));
        return resource;
    }

    private Resource<StorageLocationDTO> toResourceOffline(StorageLocationDTO location, Object... extras) {
        Resource<StorageLocationDTO> resource = new Resource<>(location);
        resourceService.addLink(resource, this.getClass(), "delete", "delete",
                                MethodParamFactory.build(String.class, location.getName()));
        return resource;
    }
}
