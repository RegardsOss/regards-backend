/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.rest;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.service.resources.IResourcesService;

/**
 *
 * Microservice resource management API
 *
 * @author Marc Sordi
 *
 */
@RestController
@RequestMapping(MicroserviceResourceController.TYPE_MAPPING)
public class MicroserviceResourceController implements IResourceController<ResourcesAccess> {

    /**
     * Controller base mapping
     */
    public static final String TYPE_MAPPING = "/resources/microservices/{microservicename}";

    /**
     * Root to retreive resources by microservice and controller name
     */
    public static final String CONTROLLERS_MAPPING = "/controllers";

    /**
     * Root to retreive resources by microservice and controller name
     */
    public static final String CONTROLLER_MAPPING = CONTROLLERS_MAPPING + "/{controllername}";

    /**
     * Resource service
     */
    @Autowired
    private IResourcesService resourceService;

    /**
     * Resource service to manage visible hateoas links
     */
    @Autowired
    private IResourceService hateoasService;

    /**
     * Retrieve authentication information
     */
    @Autowired
    private IAuthenticationResolver authResolver;

    /**
     * Retrieve the resource accesses available to the user of the given microservice
     *
     * @param pMicroserviceName
     *            microservice
     * @param pPageable
     *            pagination information
     * @param pPagedResourcesAssembler
     *            page assembler
     * @return list of user resource accesses for given microservice
     * @throws ModuleException
     *             if error occurs
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve accessible resource accesses of the user among the given microservice",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<PagedResources<Resource<ResourcesAccess>>> getAllResourceAccessesByMicroservice(
            @PathVariable("microservicename") final String pMicroserviceName, final Pageable pPageable,
            final PagedResourcesAssembler<ResourcesAccess> pPagedResourcesAssembler) throws ModuleException {
        return new ResponseEntity<>(toPagedResources(resourceService.retrieveRessources(pMicroserviceName, pPageable),
                                                     pPagedResourcesAssembler),
                HttpStatus.OK);
    }

    /**
     *
     * @param pMicroserviceName
     *            microservice name
     * @param pResourcesToRegister
     *            resource to register for the specified microservice
     * @return {@link Void}
     * @throws ModuleException
     *             if error occurs
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Register all endpoints of a microservice", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> registerMicroserviceEndpoints(
            @PathVariable("microservicename") final String pMicroserviceName,
            @RequestBody @Valid final List<ResourceMapping> pResourcesToRegister) throws ModuleException {
        resourceService.registerResources(pResourcesToRegister, pMicroserviceName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Retrieve all resource controller names for the given microservice.
     *
     * @param pMicroserviceName
     *            microservice
     * @return list of all controllers associated to the specified microservice
     */
    @RequestMapping(method = RequestMethod.GET, value = CONTROLLERS_MAPPING)
    @ResourceAccess(description = "Retrieve all resources for the given microservice and the given controller",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<String>> retrieveMicroserviceControllers(
            @PathVariable("microservicename") final String pMicroserviceName) {
        final List<String> controllers = resourceService.retrieveMicroserviceControllers(pMicroserviceName,
                                                                                         authResolver.getRole());
        controllers.sort(null);
        return new ResponseEntity<>(controllers, HttpStatus.OK);
    }

    /**
     * Retrieve all resources for the given microservice and the given controller name
     *
     * @param pMicroserviceName
     *            microservice
     * @param pControllerName
     *            controller
     * @return List of accessible resources for the specified microservice and controller
     */
    @RequestMapping(method = RequestMethod.GET, value = CONTROLLER_MAPPING)
    @ResourceAccess(description = "Retrieve all resources for the given microservice and the given controller",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<ResourcesAccess>>> retrieveMicroserviceControllerEndpoints(
            @PathVariable("microservicename") final String pMicroserviceName,
            @PathVariable("controllername") final String pControllerName) {
        final List<ResourcesAccess> resources = resourceService
                .retrieveMicroserviceControllerEndpoints(pMicroserviceName, pControllerName, authResolver.getRole());
        return new ResponseEntity<>(toResources(resources), HttpStatus.OK);
    }

    @Override
    public Resource<ResourcesAccess> toResource(ResourcesAccess pElement, Object... pExtras) {
        return hateoasService.toResource(pElement);
    }
}
