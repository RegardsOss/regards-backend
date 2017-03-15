/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.configuration.domain.Layout;
import fr.cnes.regards.modules.configuration.domain.Module;
import fr.cnes.regards.modules.configuration.service.IModuleService;

/**
 * REST controller for the microservice Access
 *
 * @author Sébastien Binda
 *
 */
@RestController
@ModuleInfo(name = "Module", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping("/applications/{applicationId}/modules")
public class ModuleController implements IResourceController<Module> {

    @Autowired
    private IModuleService service;

    @Autowired
    private IResourceService resourceService;

    /**
     * Entry point to retrieve a modules for a given application id {@link Module}.
     *
     * @return {@link Layout}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{moduleId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve an IHM module for given application", role = DefaultRole.PUBLIC)
    public HttpEntity<Resource<Module>> retrieveModule(@PathVariable("applicationId") final String pApplicationId,
            @PathVariable("applicationId") final Long pModuleId) throws EntityNotFoundException {
        final Module module = service.retrieveModule(pModuleId);
        final Resource<Module> resource = toResource(module);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to retrieve all modules for a given application id {@link Module}. Query parameter active
     * [true|false]
     *
     * @return {@link Layout}
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve IHM modules for given application", role = DefaultRole.PUBLIC)
    public HttpEntity<PagedResources<Resource<Module>>> retrieveModules(
            @PathVariable("applicationId") final String pApplicationId,
            @RequestParam(value = "active", required = false) final String pOnlyActive, final Pageable pPageable,
            final PagedResourcesAssembler<Module> pAssembler) {
        final Page<Module> modules;
        if ((pOnlyActive != null) && (Boolean.parseBoolean(pOnlyActive))) {
            modules = service.retrieveActiveModules(pApplicationId, pPageable);
        } else {
            modules = service.retrieveModules(pApplicationId, pPageable);
        }
        final PagedResources<Resource<Module>> resources = toPagedResources(modules, pAssembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Entry point to save a new ihm module.
     *
     * @return {@link Module}
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new IHM module for given application",
            role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Module>> saveModule(@PathVariable("applicationId") final String pApplicationId,
            @Valid @RequestBody final Module pModule) throws EntityInvalidException {

        if (!pModule.getApplicationId().equals(pApplicationId)) {
            throw new EntityInvalidException("Invalide application identifier for new module");
        }
        final Module module = service.saveModule(pModule);
        final Resource<Module> resource = toResource(module);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to save a new ihm module.
     *
     * @return {@link Module}
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new IHM module for given application",
            role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Module>> updateModule(@PathVariable("applicationId") final String pApplicationId,
            @Valid @RequestBody final Module pModule) throws EntityException {

        if (!pModule.getApplicationId().equals(pApplicationId)) {
            throw new EntityInvalidException("Invalide application identifier for new module");
        }
        final Module module = service.updateModule(pModule);
        final Resource<Module> resource = toResource(module);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to delete an ihm module.
     *
     * @return {@link Module}
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new IHM module for given application",
            role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Module>> deleteModule(@PathVariable("applicationId") final String pApplicationId,
            @PathVariable("moduleId") final Long pModuleId) throws EntityNotFoundException {
        service.deleteModule(pModuleId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<Module> toResource(final Module pElement, final Object... pExtras) {
        final Resource<Module> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveModule", LinkRels.SELF,
                                MethodParamFactory.build(String.class, pElement.getApplicationId()),
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "updateModule", LinkRels.UPDATE,
                                MethodParamFactory.build(String.class, pElement.getApplicationId()),
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "deleteModule", LinkRels.DELETE,
                                MethodParamFactory.build(String.class, pElement.getApplicationId()),
                                MethodParamFactory.build(Long.class, pElement.getId()));
        return resource;
    }

}
