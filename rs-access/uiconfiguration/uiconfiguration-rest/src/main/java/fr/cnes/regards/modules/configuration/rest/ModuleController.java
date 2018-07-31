/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
@RequestMapping(ModuleController.ROOT_MAPPING)
public class ModuleController implements IResourceController<Module> {

    public static final String ROOT_MAPPING = "/applications/{applicationId}/modules";

    public static final String MODULE_ID_MAPPING = "/{moduleId}";

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
    @RequestMapping(value = MODULE_ID_MAPPING, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve an IHM module for given application", role = DefaultRole.PUBLIC)
    public HttpEntity<Resource<Module>> retrieveModule(@PathVariable("applicationId") final String pApplicationId,
            @PathVariable("moduleId") final Long pModuleId) throws EntityNotFoundException {
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
            @RequestParam(value = "active", required = false) final String pOnlyActive,
            @RequestParam(value = "type", required = false) final String type, final Pageable pPageable,
            final PagedResourcesAssembler<Module> pAssembler) {
        Boolean activeBool = null;
        if (pOnlyActive != null) {
            activeBool = Boolean.parseBoolean(pOnlyActive);
        }
        final Page<Module> modules = service.retrieveModules(pApplicationId, activeBool, type, pPageable);
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
    @RequestMapping(value = MODULE_ID_MAPPING, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new IHM module for given application",
            role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Module>> updateModule(@PathVariable("applicationId") final String pApplicationId,
            @PathVariable("moduleId") final Long pModuleId, @Valid @RequestBody final Module pModule)
            throws EntityException {

        if (!pModule.getApplicationId().equals(pApplicationId)) {
            throw new EntityInvalidException("Invalide application identifier for module update");
        }

        if (!pModule.getId().equals(pModuleId)) {
            throw new EntityInvalidException("Invalide module identifier for module update");
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
    @RequestMapping(value = MODULE_ID_MAPPING, method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new IHM module for given application",
            role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Void>> deleteModule(@PathVariable("applicationId") final String pApplicationId,
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
                                MethodParamFactory.build(Long.class, pElement.getId()),
                                MethodParamFactory.build(Module.class));
        resourceService.addLink(resource, this.getClass(), "deleteModule", LinkRels.DELETE,
                                MethodParamFactory.build(String.class, pElement.getApplicationId()),
                                MethodParamFactory.build(Long.class, pElement.getId()));
        return resource;
    }

}
