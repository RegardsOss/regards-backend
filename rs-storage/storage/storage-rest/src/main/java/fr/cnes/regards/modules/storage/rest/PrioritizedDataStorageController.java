package fr.cnes.regards.modules.storage.rest;

import javax.validation.Valid;
import java.util.List;

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
import fr.cnes.regards.modules.storage.domain.database.DataStorageType;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.service.IPrioritizedDataStorageService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RestController
@RequestMapping(PrioritizedDataStorageController.BASE_PATH)
public class PrioritizedDataStorageController implements IResourceController<PrioritizedDataStorage> {

    public static final String BASE_PATH = "/storages";

    public static final String ID_PATH = "/{id}";

    public static final String UP_PATH = ID_PATH + "/up";

    public static final String DOWN_PATH = ID_PATH + "/down";

    @Autowired
    private IPrioritizedDataStorageService prioritizedDataStorageService;

    @Autowired
    private IResourceService resourceService;

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "send list of prioritized data storage of a type")
    public ResponseEntity<List<Resource<PrioritizedDataStorage>>> retrievePrioritizedDataStorages(
            @RequestParam(name = "type") DataStorageType type) throws ModuleException {
        return new ResponseEntity<>(toResources(prioritizedDataStorageService.findAllByType(type)), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "create a prioritized data storage thanks to the wrapped plugin configuration")
    public ResponseEntity<Resource<PrioritizedDataStorage>> createPrioritizedDataStorage(
            @Valid @RequestBody PrioritizedDataStorage toBeCreated) throws ModuleException {
        return new ResponseEntity<>(toResource(prioritizedDataStorageService
                                                       .create(toBeCreated.getDataStorageConfiguration())),
                                    HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET, path = ID_PATH)
    @ResourceAccess(description = "retrieve a prioritized data storage thanks to its id")
    public ResponseEntity<Resource<PrioritizedDataStorage>> retrievePrioritizedDataStorage(@PathVariable Long id)
            throws ModuleException {
        return new ResponseEntity<>(toResource(prioritizedDataStorageService.retrieve(id)), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, path = ID_PATH)
    @ResourceAccess(
            description = "delete a prioritized data storage, and the subsequent plugin configuration, thanks to its id")
    public ResponseEntity<Void> deletePrioritizedDataStorage(@PathVariable Long id) throws ModuleException {
        prioritizedDataStorageService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, path = ID_PATH)
    @ResourceAccess(description = "update a prioritized data storage by updating the subsequent plugin configuration")
    public ResponseEntity<Resource<PrioritizedDataStorage>> updatePrioritizedDataStorage(
            @PathVariable(name = "id") Long id, @Valid @RequestBody PrioritizedDataStorage updated)
            throws ModuleException {
        return new ResponseEntity<>(toResource(prioritizedDataStorageService.update(id, updated)), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, path = UP_PATH)
    @ResourceAccess(description = "increase a data storage priority")
    public ResponseEntity<Void> increaseDataStoragePriority(@PathVariable(name = "id") Long id)
            throws EntityNotFoundException {
        prioritizedDataStorageService.increasePriority(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, path = DOWN_PATH)
    @ResourceAccess(description = "decrease a data storage priority")
    public ResponseEntity<Void> decreaseDataStoragePriority(@PathVariable(name = "id") Long id)
            throws EntityNotFoundException {
        prioritizedDataStorageService.decreasePriority(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<PrioritizedDataStorage> toResource(PrioritizedDataStorage prioritizedDataStorage,
            Object... extras) {
        Resource<PrioritizedDataStorage> resource = new Resource<>(prioritizedDataStorage);
        resourceService.addLink(resource,
                                this.getClass(),
                                "retrievePrioritizedDataStorages",
                                LinkRels.LIST,
                                MethodParamFactory.build(DataStorageType.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "createPrioritizedDataStorage",
                                LinkRels.CREATE,
                                MethodParamFactory.build(PrioritizedDataStorage.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "retrievePrioritizedDataStorage",
                                LinkRels.SELF,
                                MethodParamFactory.build(Long.class, prioritizedDataStorage.getId()));
        resourceService.addLink(resource,
                                this.getClass(),
                                "updatePrioritizedDataStorage",
                                LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, prioritizedDataStorage.getId()),
                                MethodParamFactory.build(PrioritizedDataStorage.class));
        if(prioritizedDataStorageService.canDelete(prioritizedDataStorage)) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "deletePrioritizedDataStorage",
                                    LinkRels.DELETE,
                                    MethodParamFactory.build(Long.class, prioritizedDataStorage.getId()));
        }
        if (!prioritizedDataStorage.getPriority().equals(PrioritizedDataStorage.HIGHEST_PRIORITY)) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "increaseDataStoragePriority",
                                    "up",
                                    MethodParamFactory.build(Long.class, prioritizedDataStorage.getId()));
        }
        if (!prioritizedDataStorage.getPriority()
                .equals(prioritizedDataStorageService.getLowestPriority(prioritizedDataStorage.getDataStorageType()))) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "decreaseDataStoragePriority",
                                    "down",
                                    MethodParamFactory.build(Long.class, prioritizedDataStorage.getId()));
        }
        return resource;
    }
}
