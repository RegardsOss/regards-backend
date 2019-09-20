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
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedStorage;
import fr.cnes.regards.modules.storagelight.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;
import fr.cnes.regards.modules.storagelight.service.location.PrioritizedStorageService;
import fr.cnes.regards.modules.storagelight.service.location.StorageLocationService;

/**
 * @author sbinda
 *
 */
@RestController
@RequestMapping(StorageLocationController.BASE_PATH)
public class StorageLocationController implements IResourceController<StorageLocationDTO> {

    public static final String BASE_PATH = "/storages";

    public static final String ID_PATH = "/{id}";

    public static final String UP_PATH = ID_PATH + "/up";

    public static final String DOWN_PATH = ID_PATH + "/down";

    @Autowired
    private StorageLocationService service;

    @Autowired
    private PrioritizedStorageService prioriterizedStorageService;

    @Autowired
    private IResourceService resourceService;

    // TODO : Copy files
    // TODO : Delete storage
    // TODO : Empty storage (delete referenced files)

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve list of all known storage locations")
    public ResponseEntity<List<Resource<StorageLocationDTO>>> retrieve() throws ModuleException {
        return new ResponseEntity<>(toResources(service.getAllLocations()), HttpStatus.OK);
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
                    if (!location.getConfiguration().getPriority().equals(PrioritizedStorage.HIGHEST_PRIORITY)) {
                        resourceService.addLink(resource, this.getClass(), "increaseDataStoragePriority", "up",
                                                MethodParamFactory.build(String.class, location.getId()));
                    }
                    if (!location.getConfiguration().getPriority()
                            .equals(prioriterizedStorageService.getLowestPriority(StorageType.NEARLINE))) {
                        resourceService.addLink(resource, this.getClass(), "decreaseDataStoragePriority", "down",
                                                MethodParamFactory.build(String.class, location.getId()));
                    }
                    break;
                case ONLINE:
                    if (!location.getConfiguration().getPriority().equals(PrioritizedStorage.HIGHEST_PRIORITY)) {
                        resourceService.addLink(resource, this.getClass(), "increaseDataStoragePriority", "up",
                                                MethodParamFactory.build(String.class, location.getId()));
                    }
                    if (!location.getConfiguration().getPriority()
                            .equals(prioriterizedStorageService.getLowestPriority(StorageType.ONLINE))) {
                        resourceService.addLink(resource, this.getClass(), "decreaseDataStoragePriority", "down",
                                                MethodParamFactory.build(String.class, location.getId()));
                    }
                    break;
                case OFFLINE:
                default:
                    break;
            }
        }
        return resource;
    }
}
