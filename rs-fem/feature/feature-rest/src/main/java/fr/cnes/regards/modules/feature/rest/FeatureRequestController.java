/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.rest;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.service.request.IFeatureRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * REST Controller to manage feature requests
 *
 * @author Sébastien Binda
 *
 */
@RestController
@RequestMapping(FeatureRequestController.ROOT_PATH)
public class FeatureRequestController implements IResourceController<FeatureRequestDTO> {

    public static final String ROOT_PATH = "/requests";

    public static final String RETRY_PATH = "/retry";

    public static final String REQUEST_SEARCH_TYPE_PATH = "/search/{type}";

    public static final String ITEM_PATH = "/{id}";

    @Autowired
    private IFeatureRequestService featureRequestService;

    /**
     * {@link IResourceService} instance
     */
    @Autowired
    private IResourceService resourceService;

    @Operation(summary = "Get feature requests", description = "Get feature requests")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Get feature requests") })
    @RequestMapping(method = RequestMethod.GET, path = REQUEST_SEARCH_TYPE_PATH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Get features according last update date", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<FeatureRequestDTO>>> getRequests(
            @Parameter(
                    description = "Type of requests to search for") @PathVariable("type") FeatureRequestTypeEnum type,
            Pageable page, PagedResourcesAssembler<FeatureRequestDTO> assembler) {
        return new ResponseEntity<>(toPagedResources(featureRequestService.findAll(type, page), assembler),
                HttpStatus.OK);
    }

    @Operation(summary = "Delete feature request by id", description = "Delete feature request by id")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Delete feature request by id") })
    @RequestMapping(method = RequestMethod.DELETE, path = ROOT_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Delete feature request by id", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> deleteRequests() {
        // TODO : Gestion d'une payload de recherche associé aux entités à traiter.
        return null;
    }

    @Operation(summary = "Retry feature requests", description = "Retry feature requests")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Retry feature requests") })
    @RequestMapping(method = RequestMethod.DELETE, path = RETRY_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Retry feature requests", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> retryRequests() {
        // TODO : Gestion d'une payload de recherche associée aux entités à traiter.
        return null;
    }

    @Override
    public EntityModel<FeatureRequestDTO> toResource(FeatureRequestDTO element, Object... extras) {
        EntityModel<FeatureRequestDTO> resource = resourceService.toResource(element);
        // Request are deletable only if not scheduled
        if ((element.getState() == RequestState.DENIED) || (element.getState() == RequestState.ERROR)
                || (element.getState() == RequestState.SUCCESS)) {
            resourceService.addLink(resource, this.getClass(), "deleteRequest", LinkRels.DELETE,
                                    MethodParamFactory.build(String.class),
                                    MethodParamFactory.build(OffsetDateTime.class),
                                    MethodParamFactory.build(Pageable.class),
                                    MethodParamFactory.build(PagedResourcesAssembler.class));
        }
        return resource;
    }

}
