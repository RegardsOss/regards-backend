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
package fr.cnes.regards.modules.feature.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestSearchParameters;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPagedModel;
import fr.cnes.regards.modules.feature.service.request.IFeatureRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * REST Controller to manage feature requests
 *
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(FeatureRequestController.ROOT_PATH)
public class FeatureRequestController implements IResourceController<FeatureRequestDTO> {

    public static final String ROOT_PATH = "/requests";

    public static final String RETRY_TYPE_PATH = "/retry/{type}";

    public static final String DELETE_TYPE_PATH = "/delete/{type}";

    public static final String REQUEST_SEARCH_TYPE_PATH = "/search/{type}";

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
    public ResponseEntity<RequestsPagedModel<EntityModel<FeatureRequestDTO>>> getRequests(
        @Parameter(description = "Type of requests to search for") @PathVariable("type") FeatureRequestTypeEnum type,
        FeatureRequestSearchParameters parameters,
        Pageable page) {
        FeatureRequestsSelectionDTO selection = FeatureRequestsSelectionDTO.build().withFilters(parameters);
        return new ResponseEntity<>(toResources(featureRequestService.findAll(type, selection, page), type),
                                    HttpStatus.OK);
    }

    @Operation(summary = "Delete feature requests by selection", description =
        "Delete feature requests by selection. Synchronous process, so the number of request handled is limited. "
        + "Information about number of requests handled is returned in the response.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Delete feature requests by selection") })
    @RequestMapping(method = RequestMethod.DELETE, path = DELETE_TYPE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Delete feature request by selection", role = DefaultRole.EXPLOIT)
    public ResponseEntity<RequestHandledResponse> deleteRequests(
        @Parameter(description = "Type of requests to search for") @PathVariable("type") FeatureRequestTypeEnum type,
        @Parameter(description = "Requests selection") @Valid @RequestBody FeatureRequestsSelectionDTO selection) {
        return new ResponseEntity<RequestHandledResponse>(featureRequestService.delete(type, selection), HttpStatus.OK);
    }

    @Operation(summary = "Retry feature requests by selection", description =
        "Retry feature requests by selection. Synchronous process, so the number of request handled is limited. "
        + "Information about number of requests handled is returned in the response.\")")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Retry feature requests by selection") })
    @RequestMapping(method = RequestMethod.POST, path = RETRY_TYPE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Retry feature requests", role = DefaultRole.EXPLOIT)
    public ResponseEntity<RequestHandledResponse> retryRequests(
        @Parameter(description = "Type of requests to search for") @PathVariable("type") FeatureRequestTypeEnum type,
        @Parameter(description = "Requests selection") @Valid @RequestBody FeatureRequestsSelectionDTO selection) {
        return new ResponseEntity<RequestHandledResponse>(featureRequestService.retry(type, selection), HttpStatus.OK);
    }

    /**
     * Format response with HATEOAS
     *
     * @param type
     */
    private RequestsPagedModel<EntityModel<FeatureRequestDTO>> toResources(RequestsPage<FeatureRequestDTO> requestsPage,
                                                                           FeatureRequestTypeEnum type) {
        RequestsPagedModel<EntityModel<FeatureRequestDTO>> pagedResource = RequestsPagedModel.wrap(requestsPage.getContent(),
                                                                                                   new PagedModel.PageMetadata(
                                                                                                       requestsPage.getSize(),
                                                                                                       requestsPage.getNumber(),
                                                                                                       requestsPage.getTotalElements(),
                                                                                                       requestsPage.getTotalPages()),
                                                                                                   requestsPage.getInfo());
        pagedResource.getContent().forEach(resource -> addLinks(resource));

        // Add global links for entire search selection
        if (pagedResource.getInfo().getNbErrors() > 0) {
            resourceService.addLink(pagedResource,
                                    this.getClass(),
                                    "deleteRequests",
                                    LinkRels.DELETE,
                                    MethodParamFactory.build(FeatureRequestTypeEnum.class, type),
                                    MethodParamFactory.build(FeatureRequestsSelectionDTO.class));
            resourceService.addLink(pagedResource,
                                    this.getClass(),
                                    "retryRequests",
                                    LinkRelation.of("retry"),
                                    MethodParamFactory.build(FeatureRequestTypeEnum.class, type),
                                    MethodParamFactory.build(FeatureRequestsSelectionDTO.class));
        }

        return pagedResource;
    }

    private void addLinks(EntityModel<FeatureRequestDTO> resource) {
        // Request are deletable only if not scheduled
        if (!resource.getContent().isProcessing()) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "deleteRequests",
                                    LinkRels.DELETE,
                                    MethodParamFactory.build(FeatureRequestTypeEnum.class,
                                                             FeatureRequestTypeEnum.valueOf(resource.getContent()
                                                                                                    .getType())),
                                    MethodParamFactory.build(FeatureRequestsSelectionDTO.class));
            if (resource.getContent().getStep() != FeatureRequestStep.LOCAL_DELAYED) {
                resourceService.addLink(resource,
                                        this.getClass(),
                                        "retryRequests",
                                        LinkRelation.of("retry"),
                                        MethodParamFactory.build(FeatureRequestTypeEnum.class,
                                                                 FeatureRequestTypeEnum.valueOf(resource.getContent()
                                                                                                        .getType())),
                                        MethodParamFactory.build(FeatureRequestsSelectionDTO.class));
            }
        }
    }

    @Override
    public EntityModel<FeatureRequestDTO> toResource(FeatureRequestDTO element, Object... extras) {
        EntityModel<FeatureRequestDTO> resource = resourceService.toResource(element);
        addLinks(resource);
        return resource;
    }

}
