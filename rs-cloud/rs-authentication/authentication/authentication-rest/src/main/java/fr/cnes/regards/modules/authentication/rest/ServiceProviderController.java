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
package fr.cnes.regards.modules.authentication.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.swagger.autoconfigure.PageableQueryParam;
import fr.cnes.regards.modules.authentication.domain.data.Authentication;
import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import fr.cnes.regards.modules.authentication.domain.dto.ServiceProviderDto;
import fr.cnes.regards.modules.authentication.domain.exception.serviceprovider.ServiceProviderPluginIllegalParameterException;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationParams;
import fr.cnes.regards.modules.authentication.domain.service.IServiceProviderAuthenticationService;
import fr.cnes.regards.modules.authentication.domain.service.IServiceProviderCrudService;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.OpenIdAuthenticationParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.function.Function;

import static com.google.common.base.Predicates.instanceOf;
import static io.vavr.API.$;
import static io.vavr.API.Case;

@RestController
public class ServiceProviderController implements IResourceController<ServiceProviderDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderController.class);

    public static final String PATH_SERVICE_PROVIDERS = "serviceproviders";

    public static final String PATH_SERVICE_PROVIDER_BY_NAME = "/serviceproviders/{name}";

    public static final String PATH_AUTHENTICATE = "/serviceproviders/{name}/authenticate";

    public static final String PATH_DEAUTHENTICATE = "/serviceproviders/{name}/deauthenticate";

    public static final String PATH_VERIFY_AUTHENTICATION = "/serviceproviders/verify";

    @Autowired
    private IServiceProviderCrudService serviceProviderCrud;

    @Autowired
    private IServiceProviderAuthenticationService serviceProviderAuthentication;

    @Autowired
    private IResourceService resourceService;

    @Operation(summary = "Retrieves the list of service providers.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successful response."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         useReturnTypeSchema = true,
                                         content = { @Content(mediaType = "application/html") }) })
    @GetMapping(value = PATH_SERVICE_PROVIDERS)
    @ResourceAccess(description = "Retrieves the list of service providers.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<PagedModel<EntityModel<ServiceProviderDto>>> getServiceProviders(
        @PageableQueryParam @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<ServiceProviderDto> assembler) throws ModuleException {
        return serviceProviderCrud.findAll(pageable)
                                  .map(page -> page.map(ServiceProviderDto::new))
                                  .map(page -> new ResponseEntity<>(toPagedResources(page, assembler), HttpStatus.OK))
                                  .getOrElseThrow((Function<Throwable, ModuleException>) ModuleException::new);
    }

    @Operation(summary = "Creates a new service provider.")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Service provider successfully created."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         useReturnTypeSchema = true,
                                         content = { @Content(mediaType = "application/html") }) })
    @PostMapping(path = PATH_SERVICE_PROVIDERS)
    @ResourceAccess(description = "Creates a new service provider.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<EntityModel<ServiceProviderDto>> saveServiceProvider(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New service provider to create.",
                                                              content = @Content(schema = @Schema(implementation = ServiceProviderDto.class)))
        @Valid @RequestBody ServiceProviderDto serviceProvider) {
        //noinspection unchecked
        return serviceProviderCrud.save(serviceProvider.toDomain())
                                  .map(ServiceProviderDto::new)
                                  .map(sp -> new ResponseEntity<>(toResource(sp), HttpStatus.CREATED))
                                  .mapFailure(Case($(), (Function<Throwable, ModuleException>) ModuleException::new))
                                  .get();
    }

    @Operation(summary = "Updates an existing service provider.")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Service provider successfully updated."),
                            @ApiResponse(responseCode = "404", description = "Service provider does not exists."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         useReturnTypeSchema = true,
                                         content = { @Content(mediaType = "application/html") }) })
    @PutMapping(path = PATH_SERVICE_PROVIDER_BY_NAME)
    @ResourceAccess(description = "Update service provider.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<EntityModel<ServiceProviderDto>> updateServiceProvider(@PathVariable("name") String name,
                                                                                 @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                                                     description = "New service "
                                                                                                   + "provider to "
                                                                                                   + "update.",
                                                                                     content = @Content(schema = @Schema(
                                                                                         implementation = ServiceProviderDto.class)))
                                                                                 @Valid @RequestBody
                                                                                 ServiceProviderDto serviceProvider) {
        //noinspection unchecked
        return serviceProviderCrud.update(name, serviceProvider.toDomain())
                                  .map(ServiceProviderDto::new)
                                  .map(sp -> new ResponseEntity<>(toResource(sp), HttpStatus.CREATED))
                                  .mapFailure(Case($(), (Function<Throwable, ModuleException>) ModuleException::new))
                                  .get();
    }

    @Operation(summary = "Retrieves an existing service provider.")
    @ApiResponses(value = { @ApiResponse(responseCode = "201",
                                         description = "Service provider successfully retrieved."),
                            @ApiResponse(responseCode = "404", description = "Service provider does not exists."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         useReturnTypeSchema = true,
                                         content = { @Content(mediaType = "application/html") }) })
    @GetMapping(value = PATH_SERVICE_PROVIDER_BY_NAME)
    @ResourceAccess(description = "Retrieve the service provider.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<EntityModel<ServiceProviderDto>> getServiceProvider(@PathVariable("name") String name) {
        //noinspection unchecked
        return serviceProviderCrud.findByName(name)
                                  .map(ServiceProviderDto::new)
                                  .map(sp -> new ResponseEntity<>(toResource(sp), HttpStatus.OK))
                                  .mapFailure(Case($(instanceOf(NoSuchElementException.class)),
                                                   ex -> new EntityNotFoundException(name, ServiceProvider.class)),
                                              Case($(), (Function<Throwable, ModuleException>) ModuleException::new))
                                  .get();
    }

    @Operation(summary = "Deletes an existing service provider.")
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Service provider successfully deleted."),
                            @ApiResponse(responseCode = "404", description = "Service provider does not exists."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         useReturnTypeSchema = true,
                                         content = { @Content(mediaType = "application/html") }) })
    @DeleteMapping(value = PATH_SERVICE_PROVIDER_BY_NAME)
    @ResourceAccess(description = "Delete the service provider.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> deleteServiceProvider(@PathVariable("name") String name) throws ModuleException {
        return serviceProviderCrud.delete(name)
                                  .map(u -> new ResponseEntity<Void>(HttpStatus.NO_CONTENT))
                                  .getOrElseThrow((Function<Throwable, ModuleException>) ModuleException::new);
    }

    @Operation(summary = "Authenticate with the given service provider.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Authentication success."),
                            @ApiResponse(responseCode = "401", description = "Authentication unauthorized."),
                            @ApiResponse(responseCode = "404", description = "Service provider does not exists."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         useReturnTypeSchema = true,
                                         content = { @Content(mediaType = "application/html") }) })
    @PostMapping(value = PATH_AUTHENTICATE)
    @ResourceAccess(description = "Authenticate with the given service provider.", role = DefaultRole.PUBLIC)
    public ResponseEntity<Authentication> authenticate(@PathVariable("name") String name,
                                                       @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Authentication credentials.",
                                                                                                             content = @Content(
                                                                                                                 schema = @Schema(
                                                                                                                     implementation = OpenIdAuthenticationParams.class)))
                                                       @RequestBody ServiceProviderAuthenticationParams params)
        throws ModuleException {
        return serviceProviderAuthentication.authenticate(name, params)
                                            .map(ResponseEntity::ok)
                                            .recover(ServiceProviderPluginIllegalParameterException.class,
                                                     ex -> ResponseEntity.badRequest().build())
                                            .recover(AuthenticationException.class, ex -> {
                                                LOGGER.error(ex.getMessage(), ex);
                                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                                            })
                                            .getOrElseThrow((Function<Throwable, ModuleException>) ModuleException::new);
    }

    @Operation(summary = "Logout with the given service provider.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Logout success."),
                            @ApiResponse(responseCode = "404", description = "Service provider does not exists."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         useReturnTypeSchema = true,
                                         content = { @Content(mediaType = "application/html") }) })
    @PostMapping(value = PATH_DEAUTHENTICATE)
    @ResourceAccess(description = "Deauthenticate from the given service provider.", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> deauthenticate(@PathVariable("name") String name) throws ModuleException {
        return serviceProviderAuthentication.deauthenticate(name)
                                            .map(u -> new ResponseEntity<Void>(HttpStatus.OK))
                                            .getOrElseThrow((Function<Throwable, ModuleException>) ModuleException::new);
    }

    @Operation(summary = "Verify and authenticate token through service providers.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Logout success."),
                            @ApiResponse(responseCode = "401", description = "Authentication unauthorized."),
                            @ApiResponse(responseCode = "404", description = "Service provider does not exists."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         useReturnTypeSchema = true,
                                         content = { @Content(mediaType = "application/html") }) })
    @GetMapping(value = PATH_VERIFY_AUTHENTICATION)
    @ResourceAccess(description = "Verify and authenticate token through service providers.",
                    role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Authentication> verifyAndAuthenticate(
        @Parameter(description = "External token from service provider to authenticate with.") @RequestParam
        String externalToken) throws ModuleException {
        return serviceProviderAuthentication.verifyAndAuthenticate(externalToken)
                                            .map(ResponseEntity::ok)
                                            .recover(ServiceProviderPluginIllegalParameterException.class,
                                                     ex -> ResponseEntity.badRequest().build())
                                            .recover(AuthenticationException.class,
                                                     ex -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
                                            .getOrElseThrow((Function<Throwable, ModuleException>) ModuleException::new);
    }

    @Override
    public EntityModel<ServiceProviderDto> toResource(final ServiceProviderDto element, final Object... extras) {
        EntityModel<ServiceProviderDto> resource = resourceService.toResource(element);
        if ((element != null)) {
            resource = resourceService.toResource(element);
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "getServiceProviders",
                                    LinkRels.LIST,
                                    MethodParamFactory.build(Pageable.class),
                                    MethodParamFactory.build(PagedResourcesAssembler.class));
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "saveServiceProvider",
                                    LinkRels.CREATE,
                                    MethodParamFactory.build(ServiceProviderDto.class));
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "saveServiceProvider",
                                    LinkRels.UPDATE,
                                    MethodParamFactory.build(ServiceProviderDto.class));
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "getServiceProvider",
                                    LinkRels.SELF,
                                    MethodParamFactory.build(String.class, element.getName()));
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "deleteServiceProvider",
                                    LinkRels.DELETE,
                                    MethodParamFactory.build(String.class, element.getName()));
        }
        return resource;
    }
}
