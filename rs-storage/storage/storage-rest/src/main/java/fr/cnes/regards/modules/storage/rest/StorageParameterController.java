package fr.cnes.regards.modules.storage.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.storage.domain.parameter.StorageParameter;
import fr.cnes.regards.modules.storage.service.parameter.IStorageParameterService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RestController
@RequestMapping(StorageParameterController.ROOT_PATH)
public class StorageParameterController implements IResourceController<StorageParameter> {

    public static final String ROOT_PATH = "/storage/parameters";

    private static final String PATH_BY_NAME = "/{name}";

    private static final String PATH_BY_ID = "/{id}";

    @Autowired
    private IStorageParameterService storageParameterService;

    @Autowired
    private IResourceService resourceService;

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "endpoint allowing to retrieve all configurable storage parameters")
    @ResponseBody
    public HttpEntity<List<Resource<StorageParameter>>> retrieveAll() {
        return new ResponseEntity<>(toResources(storageParameterService.retrieveAll()), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, path = PATH_BY_NAME)
    @ResourceAccess(description = "endpoint allowing to retrieve one storage parameter by its name")
    @ResponseBody
    public HttpEntity<Resource<StorageParameter>> retrieveByName(@PathVariable(name = "name") String parameterName)
            throws EntityNotFoundException {
        return new ResponseEntity<>(toResource(storageParameterService.retrieveByName(parameterName)), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "endpoint allowing to create a new storage parameters")
    @ResponseBody
    public HttpEntity<Resource<StorageParameter>> create(@RequestBody StorageParameter storageParameter)
            throws EntityAlreadyExistsException {
        return new ResponseEntity<>(toResource(storageParameterService.create(storageParameter)), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.DELETE, path = PATH_BY_NAME)
    @ResourceAccess(description = "endpoint allowing to delete one storage parameter by its name")
    @ResponseBody
    public HttpEntity<Resource<StorageParameter>> deleteByName(@PathVariable(name = "name") String parameterName) {
        storageParameterService.delete(parameterName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, path = PATH_BY_ID)
    @ResourceAccess(description = "endpoint allowing to update one storage parameter by its id")
    @ResponseBody
    public HttpEntity<Resource<StorageParameter>> updateById(@PathVariable(name = "id") Long parameterId,
            @RequestBody StorageParameter updated)
            throws EntityInconsistentIdentifierException, EntityNotFoundException {
        return new ResponseEntity<>(toResource(storageParameterService.update(parameterId, updated)), HttpStatus.OK);
    }

    @Override
    public Resource<StorageParameter> toResource(StorageParameter pElement, Object... pExtras) {
        Resource<StorageParameter> resource = new Resource<>(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveByName", LinkRels.SELF,
                                MethodParamFactory.build(String.class, pElement.getName()));
        resourceService.addLink(resource, this.getClass(), "retrieveAll", LinkRels.LIST);
        resourceService.addLink(resource, this.getClass(), "deleteByName", LinkRels.DELETE,
                                MethodParamFactory.build(String.class, pElement.getName()));
        resourceService.addLink(resource, this.getClass(), "updateById", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, pElement.getId()),
                                MethodParamFactory.build(StorageParameter.class));
        resourceService.addLink(resource, this.getClass(), "create", LinkRels.CREATE,
                                MethodParamFactory.build(StorageParameter.class));
        return resource;
    }
}
