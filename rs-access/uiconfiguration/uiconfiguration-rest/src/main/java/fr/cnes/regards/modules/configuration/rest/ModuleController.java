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
package fr.cnes.regards.modules.configuration.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
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
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.configuration.domain.Module;
import fr.cnes.regards.modules.configuration.domain.UILayout;
import fr.cnes.regards.modules.configuration.service.IModuleService;

/**
 * REST controller for the microservice Access
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(ModuleController.ROOT_MAPPING)
public class ModuleController implements IResourceController<Module> {

    public static final String ROOT_MAPPING = "/applications/{applicationId}/modules";

    public static final String MODULE_ID_MAPPING = "/{moduleId}";

    public static final String MAP_CONFIG = MODULE_ID_MAPPING + "/map";

    @Autowired
    private IModuleService service;

    @Autowired
    private IResourceService resourceService;

    /**
     * Entry point to retrieve a modules for a given application id {@link Module}.
     * @param applicationId
     * @param moduleId
     * @return {@link UILayout}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = MODULE_ID_MAPPING, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve an IHM module for given application", role = DefaultRole.PUBLIC)
    public HttpEntity<EntityModel<Module>> retrieveModule(@PathVariable("applicationId") String applicationId,
            @PathVariable("moduleId") Long moduleId) throws EntityNotFoundException {
        Module module = service.retrieveModule(moduleId);
        EntityModel<Module> resource = toResource(module);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to retrieve all modules for a given application id {@link Module}. Query parameter active
     * [true|false]
     * @param applicationId
     * @param onlyActive
     * @param type
     * @param pageable
     * @param assembler
     * @return {@link UILayout}
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve IHM modules for given application", role = DefaultRole.PUBLIC)
    public HttpEntity<PagedModel<EntityModel<Module>>> retrieveModules(
            @PathVariable("applicationId") String applicationId,
            @RequestParam(value = "active", required = false) String onlyActive,
            @RequestParam(value = "type", required = false) String type,
            @SortDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<Module> assembler) {
        Boolean activeBool = (onlyActive != null) ? Boolean.parseBoolean(onlyActive) : null;
        Page<Module> modules = service.retrieveModules(applicationId, activeBool, type, pageable);
        PagedModel<EntityModel<Module>> resources = toPagedResources(modules, assembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Entry point to save a new ihm module.
     * @param applicationId
     * @param module
     * @return {@link Module}
     * @throws EntityInvalidException
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new IHM module for given application",
            role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<EntityModel<Module>> saveModule(@PathVariable("applicationId") String applicationId,
            @Valid @RequestBody Module module) throws EntityInvalidException {

        if (!module.getApplicationId().equals(applicationId)) {
            throw new EntityInvalidException("Invalid application identifier for new module");
        }
        return new ResponseEntity<>(toResource(service.saveModule(module)), HttpStatus.OK);
    }

    /**
     * Entry point to save a new ihm module.
     * @param applicationId
     * @param moduleId
     * @param module
     * @return {@link Module}
     * @throws EntityException
     */
    @RequestMapping(value = MODULE_ID_MAPPING, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new IHM module for given application",
            role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<EntityModel<Module>> updateModule(@PathVariable("applicationId") String applicationId,
            @PathVariable("moduleId") Long moduleId, @Valid @RequestBody Module module) throws EntityException {
        if (!module.getApplicationId().equals(applicationId)) {
            throw new EntityInvalidException("Invalid application identifier for module update");
        }
        if (!module.getId().equals(moduleId)) {
            throw new EntityInvalidException("Invalid module identifier for module update");
        }
        return new ResponseEntity<>(toResource(service.updateModule(module)), HttpStatus.OK);
    }

    /**
     * Entry point to delete an ihm module.
     * @param applicationId
     * @param moduleId
     * @return {@link Module}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = MODULE_ID_MAPPING, method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new IHM module for given application",
            role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<EntityModel<Void>> deleteModule(@PathVariable("applicationId") String applicationId,
            @PathVariable("moduleId") Long moduleId) throws EntityNotFoundException {
        service.deleteModule(moduleId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<Module> toResource(final Module pElement, final Object... pExtras) {
        final EntityModel<Module> resource = resourceService.toResource(pElement);
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
