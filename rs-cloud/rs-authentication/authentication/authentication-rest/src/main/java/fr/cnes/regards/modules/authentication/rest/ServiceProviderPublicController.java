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
import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import fr.cnes.regards.modules.authentication.domain.dto.ServiceProviderPublicDto;
import fr.cnes.regards.modules.authentication.domain.service.IServiceProviderCrudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;
import java.util.function.Function;

import static com.google.common.base.Predicates.instanceOf;
import static io.vavr.API.$;
import static io.vavr.API.Case;

@RestController
public class ServiceProviderPublicController implements IResourceController<ServiceProviderPublicDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderPublicController.class);

    public static final String PATH_SERVICE_PROVIDER_PUBLIC = "/serviceproviders/public";

    public static final String PATH_SERVICE_PROVIDER_PUBLIC_BY_NAME = "/serviceproviders/public/{name}";

    @Autowired
    private IServiceProviderCrudService serviceProviderCrud;

    @Autowired
    private IResourceService resourceService;

    @Operation(summary = "Retrieves the list of service providers without associated plugin configuration.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successful response."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         useReturnTypeSchema = true,
                                         content = { @Content(mediaType = "application/html") }) })
    @ResponseBody
    @GetMapping(value = PATH_SERVICE_PROVIDER_PUBLIC)
    @ResourceAccess(description = "Retrieves the list of service providers without associated plugin configuration.",
                    role = DefaultRole.PUBLIC)
    public ResponseEntity<PagedModel<EntityModel<ServiceProviderPublicDto>>> getServiceProvidersPublic(
        @PageableQueryParam @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<ServiceProviderPublicDto> assembler) throws ModuleException {
        return serviceProviderCrud.findAll(pageable)
                                  .map(page -> page.map(ServiceProviderPublicDto::new))
                                  .map(page -> new ResponseEntity<>(toPagedResources(page, assembler), HttpStatus.OK))
                                  .getOrElseThrow((Function<Throwable, ModuleException>) ModuleException::new);
    }

    @Operation(summary = "Retrieves an existing service provider without associated plugin configuration.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successful response."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         useReturnTypeSchema = true,
                                         content = { @Content(mediaType = "application/html") }) })
    @ResponseBody
    @GetMapping(value = PATH_SERVICE_PROVIDER_PUBLIC_BY_NAME)
    @ResourceAccess(description = "Retrieve the service provider.", role = DefaultRole.PUBLIC)
    public ResponseEntity<EntityModel<ServiceProviderPublicDto>> getServiceProvider(@PathVariable("name") String name) {
        //noinspection unchecked
        return serviceProviderCrud.findByName(name)
                                  .map(ServiceProviderPublicDto::new)
                                  .map(sp -> new ResponseEntity<>(toResource(sp), HttpStatus.OK))
                                  .mapFailure(Case($(instanceOf(NoSuchElementException.class)),
                                                   ex -> new EntityNotFoundException(name, ServiceProvider.class)),
                                              Case($(), (Function<Throwable, ModuleException>) ModuleException::new))
                                  .get();
    }

    @Override
    public EntityModel<ServiceProviderPublicDto> toResource(final ServiceProviderPublicDto element,
                                                            final Object... extras) {
        EntityModel<ServiceProviderPublicDto> resource = resourceService.toResource(element);
        if ((element != null)) {
            resource = resourceService.toResource(element);
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "getServiceProvidersPublic",
                                    LinkRels.LIST,
                                    MethodParamFactory.build(Pageable.class),
                                    MethodParamFactory.build(PagedResourcesAssembler.class));
        }
        return resource;
    }
}
