/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.service.resources.IResourcesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Microservice resource management API
 *
 * @author Marc Sordi
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
    private final IResourcesService resourceService;

    /**
     * Resource service to manage visible hateoas links
     */
    private final IResourceService hateoasService;

    /**
     * Retrieve authentication information
     */
    private final IAuthenticationResolver authResolver;

    public MicroserviceResourceController(IResourcesService resourceService,
                                          IResourceService hateoasService,
                                          IAuthenticationResolver authResolver) {
        this.resourceService = resourceService;
        this.hateoasService = hateoasService;
        this.authResolver = authResolver;
    }

    /**
     * Retrieve the resource accesses available to the user of the given microservice
     *
     * @param microserviceName microservice
     * @param pageable         pagination information
     * @param assembler        page assembler
     * @return list of user resource accesses for given microservice
     * @throws ModuleException if error occurs
     */
    @GetMapping
    @ResourceAccess(description = "Retrieve accessible resource accesses of the user among the given microservice",
                    role = DefaultRole.PUBLIC)
    public ResponseEntity<PagedModel<EntityModel<ResourcesAccess>>> getAllResourceAccessesByMicroservice(
        @PathVariable("microservicename") String microserviceName,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<ResourcesAccess> assembler) throws ModuleException {
        return new ResponseEntity<>(toPagedResources(resourceService.retrieveRessources(microserviceName, pageable),
                                                     assembler), HttpStatus.OK);
    }

    /**
     * @param microserviceName    microservice name
     * @param toRegisterResources resource to register for the specified microservice
     * @return {@link Void}
     * @throws ModuleException if error occurs
     */
    @PostMapping
    @ResourceAccess(description = "Register all endpoints of a microservice", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> registerMicroserviceEndpoints(@PathVariable("microservicename") String microserviceName,
                                                              @RequestBody @Valid
                                                              List<ResourceMapping> toRegisterResources)
        throws ModuleException {
        resourceService.registerResources(toRegisterResources, microserviceName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Retrieve all resource controller names for the given microservice.
     *
     * @param microserviceName microservice
     * @return list of all controllers associated to the specified microservice
     */
    @GetMapping(value = CONTROLLERS_MAPPING)
    @ResourceAccess(description = "Retrieve all resources for the given microservice and the given controller",
                    role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<String>> retrieveMicroserviceControllers(
        @PathVariable("microservicename") String microserviceName) {
        final List<String> controllers = resourceService.retrieveMicroserviceControllers(microserviceName,
                                                                                         authResolver.getRole());
        controllers.sort(null);
        return new ResponseEntity<>(controllers, HttpStatus.OK);
    }

    /**
     * Retrieve all resources for the given microservice and the given controller name
     *
     * @param microserviceName microservice
     * @param controllerName   controller
     * @return List of accessible resources for the specified microservice and controller
     */
    @GetMapping(value = CONTROLLER_MAPPING)
    @ResourceAccess(description = "Retrieve all resources for the given microservice and the given controller",
                    role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<EntityModel<ResourcesAccess>>> retrieveMicroserviceControllerEndpoints(
        @PathVariable("microservicename") String microserviceName,
        @PathVariable("controllername") String controllerName) {
        final List<ResourcesAccess> resources = resourceService.retrieveMicroserviceControllerEndpoints(microserviceName,
                                                                                                        controllerName,
                                                                                                        authResolver.getRole());
        return new ResponseEntity<>(toResources(resources), HttpStatus.OK);
    }

    @DeleteMapping
    @Operation(summary = "Delete microservice resources for all active tenants",
               description = "Delete microservice resources for all active tenants")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Resources successfully deleted."),
                            @ApiResponse(responseCode = "403", description = "Access denied.") })
    @ResourceAccess(description = "Delete microservice resources for all active tenants",
                    role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> deleteMicroserviceEndpoints(@PathVariable("microservicename") String microserviceName) {
        resourceService.removeMicroserviceResourcesForAllTenant(microserviceName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<ResourcesAccess> toResource(ResourcesAccess element, Object... extras) {
        return hateoasService.toResource(element);
    }
}
