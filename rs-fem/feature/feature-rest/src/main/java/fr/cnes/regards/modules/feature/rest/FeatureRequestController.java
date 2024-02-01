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
import fr.cnes.regards.framework.swagger.autoconfigure.PageableQueryParam;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPagedModel;
import fr.cnes.regards.modules.feature.service.abort.FeatureRequestAbortService;
import fr.cnes.regards.modules.feature.service.request.IFeatureRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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

    private final  IFeatureRequestService featureRequestService;

    /**
     * {@link IResourceService} instance
     */
    private final IResourceService resourceService;

    /**
     * Minimum delay before aborting running request. Temporary workaround to be removed when abort functionality
     * will be deleted.
     */
    private final int abortDelayInHours;

    public FeatureRequestController(IFeatureRequestService featureRequestService,
                                    IResourceService resourceService,
                                    @Value("${regards.feature.abort.delay.hours:1}") int abortDelayInHours) {
        this.featureRequestService = featureRequestService;
        this.resourceService = resourceService;
        this.abortDelayInHours = abortDelayInHours;
    }

    @PostMapping(path = REQUEST_SEARCH_TYPE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get feature requests", description = "Return a page of feature requests according criterias")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "All feature requests were retrieved.") })
    @ResourceAccess(description = "Endpoint to retrieve all feature requests according criterias",
                    role = DefaultRole.EXPLOIT)
    public ResponseEntity<RequestsPagedModel<EntityModel<FeatureRequestDTO>>> searchFeatureRequests(
        @Parameter(description = "Type of feature requests to search for") @PathVariable("type")
        FeatureRequestTypeEnum type,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Set of search criterias.",
                                                              content = @Content(schema = @Schema(implementation = SearchFeatureRequestParameters.class)))
        @Parameter(description = "Filter criterias for feature requests") @RequestBody
        SearchFeatureRequestParameters filters,
        @PageableQueryParam @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable page) {

        return new ResponseEntity<>(toResources(featureRequestService.findAll(type, filters, page), type),
                                    HttpStatus.OK);
    }

    @Operation(summary = "Delete feature requests by selection",
               description =
                   "Delete feature requests by selection. Synchronous process, so the number of request handled is limited. "
                   + "Information about number of requests handled is returned in the response.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Delete feature requests by selection") })
    @RequestMapping(method = RequestMethod.DELETE, path = DELETE_TYPE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Delete feature request by selection", role = DefaultRole.EXPLOIT)
    public ResponseEntity<RequestHandledResponse> deleteRequests(
        @Parameter(description = "Type of requests to search for") @PathVariable("type") FeatureRequestTypeEnum type,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Set of search criterias.",
                                                              content = @Content(schema = @Schema(implementation = SearchFeatureRequestParameters.class)))
        @Parameter(description = "Requests selection") @Valid @RequestBody SearchFeatureRequestParameters selection) {
        return new ResponseEntity<RequestHandledResponse>(featureRequestService.delete(type, selection), HttpStatus.OK);
    }

    @Operation(summary = "Retry feature requests by selection",
               description =
                   "Retry feature requests by selection. Synchronous process, so the number of request handled is limited. "
                   + "Information about number of requests handled is returned in the response.\")")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Retry feature requests by selection") })
    @RequestMapping(method = RequestMethod.POST, path = RETRY_TYPE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Retry feature requests", role = DefaultRole.EXPLOIT)
    public ResponseEntity<RequestHandledResponse> retryRequests(
        @Parameter(description = "Type of requests to search for") @PathVariable("type") FeatureRequestTypeEnum type,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Set of search criterias.",
                                                              content = @Content(schema = @Schema(implementation = SearchFeatureRequestParameters.class)))
        @Parameter(description = "Requests selection") @Valid @RequestBody SearchFeatureRequestParameters selection) {
        return new ResponseEntity<>(featureRequestService.retry(type, selection), HttpStatus.OK);
    }

    /**
     * Format response with HATEOAS
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
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        pagedResource.getContent().forEach(requestDto -> this.addLinks(requestDto, start));

        // Add global links for entire search selection
        if (pagedResource.getInfo().getNbErrors() > 0) {
            resourceService.addLink(pagedResource,
                                    this.getClass(),
                                    "deleteRequests",
                                    LinkRels.DELETE,
                                    MethodParamFactory.build(FeatureRequestTypeEnum.class, type),
                                    MethodParamFactory.build(SearchFeatureRequestParameters.class));
            resourceService.addLink(pagedResource,
                                    this.getClass(),
                                    "retryRequests",
                                    LinkRelation.of("retry"),
                                    MethodParamFactory.build(FeatureRequestTypeEnum.class, type),
                                    MethodParamFactory.build(SearchFeatureRequestParameters.class));
        }

        return pagedResource;
    }

    private void addLinks(EntityModel<FeatureRequestDTO> resource, OffsetDateTime start) {
        FeatureRequestDTO featureRequest = resource.getContent();
        if (featureRequest == null) {
            return;
        }
        FeatureRequestTypeEnum requestType = FeatureRequestTypeEnum.valueOf(featureRequest.getType());
        // Request are deletable only if not scheduled
        if (!featureRequest.isProcessing()) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "deleteRequests",
                                    LinkRels.DELETE,
                                    MethodParamFactory.build(FeatureRequestTypeEnum.class, requestType),
                                    MethodParamFactory.build(SearchFeatureRequestParameters.class));
            if (featureRequest.getStep() != FeatureRequestStep.LOCAL_DELAYED) {
                resourceService.addLink(resource,
                                        this.getClass(),
                                        "retryRequests",
                                        LinkRelation.of("retry"),
                                        MethodParamFactory.build(FeatureRequestTypeEnum.class, requestType),
                                        MethodParamFactory.build(SearchFeatureRequestParameters.class));
            }
        }

        // Add abort link only if request can be aborted, i.e., delay before aborting request is valid, request
        // state, type and step are valid.
        // This is a temporary option that will be removed later
        if (featureRequest.getRegistrationDate().plusHours(abortDelayInHours).isBefore(start)
            && featureRequest.getState() == RequestState.GRANTED
            && FeatureRequestAbortService.STEPS_CORRELATION_TABLE.containsKey(requestType)
            && FeatureRequestAbortService.STEPS_CORRELATION_TABLE.get(requestType)
                                                                 .containsKey(featureRequest.getStep())) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "searchFeatureRequests",
                                    LinkRelation.of("abort"),
                                    MethodParamFactory.build(FeatureRequestTypeEnum.class, requestType),
                                    MethodParamFactory.build(SearchFeatureRequestParameters.class),
                                    MethodParamFactory.build(Pageable.class));
        }
    }

    @Override
    public EntityModel<FeatureRequestDTO> toResource(FeatureRequestDTO element, Object... extras) {
        EntityModel<FeatureRequestDTO> resource = resourceService.toResource(element);
        addLinks(resource, OffsetDateTime.now(ZoneOffset.UTC));
        return resource;
    }

}
