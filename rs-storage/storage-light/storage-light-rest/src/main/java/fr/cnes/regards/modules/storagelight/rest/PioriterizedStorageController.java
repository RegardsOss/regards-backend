/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedStorage;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;
import fr.cnes.regards.modules.storagelight.service.location.PrioritizedStorageService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RestController
@RequestMapping(PioriterizedStorageController.BASE_PATH)
public class PioriterizedStorageController implements IResourceController<PrioritizedStorage> {

    public static final String BASE_PATH = "/storages/configuration";

    public static final String ID_PATH = "/{id}";

    public static final String UP_PATH = ID_PATH + "/up";

    public static final String DOWN_PATH = ID_PATH + "/down";

    @Autowired
    private PrioritizedStorageService prioriterizedStorageService;

    @Autowired
    private IResourceService resourceService;

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "send list of prioritized data storage of a type")
    public ResponseEntity<List<Resource<PrioritizedStorage>>> retrievePrioritizedStorages(
            @RequestParam(name = "type") StorageType type) throws ModuleException {
        return new ResponseEntity<>(toResources(prioriterizedStorageService.search(type)), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "create a prioritized data storage thanks to the wrapped plugin configuration")
    public ResponseEntity<Resource<PrioritizedStorage>> createPrioritizedStorage(
            @Valid @RequestBody PrioritizedStorage toBeCreated) throws ModuleException {
        return new ResponseEntity<>(
                toResource(prioriterizedStorageService.create(toBeCreated.getStorageConfiguration())),
                HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET, path = ID_PATH)
    @ResourceAccess(description = "retrieve a prioritized data storage thanks to its id")
    public ResponseEntity<Resource<PrioritizedStorage>> retrievePrioritizedStorage(@PathVariable Long id)
            throws ModuleException {
        return new ResponseEntity<>(toResource(prioriterizedStorageService.retrieve(id)), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, path = ID_PATH)
    @ResourceAccess(
            description = "delete a prioritized data storage, and the subsequent plugin configuration, thanks to its id")
    public ResponseEntity<Void> deletePrioritizedStorage(@PathVariable Long id) throws ModuleException {
        prioriterizedStorageService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, path = ID_PATH)
    @ResourceAccess(description = "update a prioritized data storage by updating the subsequent plugin configuration")
    public ResponseEntity<Resource<PrioritizedStorage>> updatePrioritizedStorage(@PathVariable(name = "id") Long id,
            @Valid @RequestBody PrioritizedStorage updated) throws ModuleException {
        return new ResponseEntity<>(toResource(prioriterizedStorageService.update(id, updated)), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, path = UP_PATH)
    @ResourceAccess(description = "increase a data storage priority")
    public ResponseEntity<Void> increaseDataStoragePriority(@PathVariable(name = "id") Long id)
            throws EntityNotFoundException {
        prioriterizedStorageService.increasePriority(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, path = DOWN_PATH)
    @ResourceAccess(description = "decrease a data storage priority")
    public ResponseEntity<Void> decreaseDataStoragePriority(@PathVariable(name = "id") Long id)
            throws EntityNotFoundException {
        prioriterizedStorageService.decreasePriority(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<PrioritizedStorage> toResource(PrioritizedStorage prioritizedStorage, Object... extras) {
        Resource<PrioritizedStorage> resource = new Resource<>(prioritizedStorage);
        resourceService.addLink(resource, this.getClass(), "retrievePrioritizedStorages", LinkRels.LIST,
                                MethodParamFactory.build(StorageType.class));
        resourceService.addLink(resource, this.getClass(), "createPrioritizedStorage", LinkRels.CREATE,
                                MethodParamFactory.build(PrioritizedStorage.class));
        resourceService.addLink(resource, this.getClass(), "retrievePrioritizedStorage", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, prioritizedStorage.getId()));
        resourceService.addLink(resource, this.getClass(), "updatePrioritizedStorage", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, prioritizedStorage.getId()),
                                MethodParamFactory.build(PrioritizedStorage.class));
        resourceService.addLink(resource, this.getClass(), "deletePrioritizedStorage", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, prioritizedStorage.getId()));
        if (!prioritizedStorage.getPriority().equals(PrioritizedStorage.HIGHEST_PRIORITY)) {
            resourceService.addLink(resource, this.getClass(), "increaseDataStoragePriority", "up",
                                    MethodParamFactory.build(Long.class, prioritizedStorage.getId()));
        }
        if (!prioritizedStorage.getPriority()
                .equals(prioriterizedStorageService.getLowestPriority(prioritizedStorage.getStorageType()))) {
            resourceService.addLink(resource, this.getClass(), "decreaseDataStoragePriority", "down",
                                    MethodParamFactory.build(Long.class, prioritizedStorage.getId()));
        }
        return resource;
    }
}
