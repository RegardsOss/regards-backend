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
package fr.cnes.regards.modules.ingest.rest;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.sip.VersioningMode;
import fr.cnes.regards.modules.ingest.dto.request.ChooseVersioningRequestParameters;
import fr.cnes.regards.modules.ingest.dto.request.RequestDto;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestParameters;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * This controller manages Requests.
 *
 * @author Léo Mieulet
 */
@RestController
@RequestMapping(RequestController.TYPE_MAPPING)
public class RequestController implements IResourceController<RequestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestController.class);

    public static final String TYPE_MAPPING = "/requests";

    /**
     * Controller path to retry multiple requests using criteria
     */
    public static final String REQUEST_RETRY_PATH = "/retry";

    /**
     * Controller path to abort multiple requests using criteria
     */
    public static final String REQUEST_ABORT_PATH = "/abort";

    /**
     * Controller path to choose multiple requests, using criteria, versioning mode after {@link fr.cnes.regards.modules.ingest.domain.sip.VersioningMode#MANUAL}
     */
    public static final String VERSIONING_CHOICE_PATH = "/versioning";

    /**
     * Controller path to delete several request entities
     */
    public static final String REQUEST_DELETE_PATH = "/delete";

    private final IRequestService requestService;

    private final IResourceService resourceService;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IIngestRequestService ingestRequestService;

    public RequestController(IResourceService resourceService,
                             IRequestService requestService,
                             IRuntimeTenantResolver runtimeTenantResolver,
                             IIngestRequestService ingestRequestService) {
        this.resourceService = resourceService;
        this.requestService = requestService;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.ingestRequestService = ingestRequestService;

    }

    /**
     * Retrieve a page of ingest requests according to the given filters
     *
     * @param filters request filters
     * @return page of aip metadata respecting the constraints
     */
    @PostMapping
    @Operation(summary = "Get requests", description = "Return a page of requests matching criteria")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "All requests were retrieved.") })
    @ResourceAccess(description = "Endpoint to retrieve all requests matching criteria", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<RequestDto>>> searchRequest(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Set of search criteria.",
                                                              content = @Content(schema = @Schema(implementation = SearchRequestParameters.class)))
        @Parameter(description = "Filter criteria for requests") @RequestBody SearchRequestParameters filters,
        @Parameter(description = "Sorting and page configuration")
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<RequestDto> assembler) {

        return new ResponseEntity<>(toPagedResources(requestService.findRequestDtos(filters, pageable), assembler),
                                    HttpStatus.OK);
    }

    @PostMapping(REQUEST_RETRY_PATH)
    @Operation(summary = "Retry selected requests", description = "Retry all requests matching given search criteria")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "Retry process has been successfully initialized.") })
    @ResourceAccess(description = "Retry requests matching provided filters", role = DefaultRole.EXPLOIT)
    public void retryRequests(@io.swagger.v3.oas.annotations.parameters.RequestBody(description =
                                                                                        "Set of search criteria to find requests"
                                                                                        + " to retry",
                                                                                    content = @Content(schema = @Schema(
                                                                                        implementation = SearchRequestParameters.class)))
                              @Parameter(description = "Filter criteria for requests") @Valid @RequestBody
                              SearchRequestParameters filters) {
        LOGGER.debug("Received request to retry requests");
        requestService.scheduleRequestRetryJob(filters);
    }

    @PutMapping(VERSIONING_CHOICE_PATH)
    @Operation(summary = "Choose versioning mode",
               description = "Apply given versioning mode to all requests " + "matching given search criteria")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "Update versioning mode process has been successfully "
                                                       + "initialized.") })
    @ResourceAccess(description = "Choose versioning mode for requests matching provided filters",
                    role = DefaultRole.EXPLOIT)
    public ResponseEntity<Object> chooseVersioningMode(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Set of search criteria to find requests"
                                                                            + " to update with new versioning mode",
                                                              content = @Content(schema = @Schema(implementation = ChooseVersioningRequestParameters.class)))
        @Parameter(description = "Filter criteria for requests") @Valid @RequestBody
        ChooseVersioningRequestParameters filters) {
        if (filters.getNewVersioningMode() == VersioningMode.MANUAL) {
            return ResponseEntity.unprocessableEntity()
                                 .body("You cannot choose " + VersioningMode.MANUAL + " versioning mode!");
        }
        LOGGER.debug("Received request to retry requests");
        ingestRequestService.scheduleRequestWithVersioningMode(filters);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(REQUEST_ABORT_PATH)
    @Operation(summary = "Abort all running requests", description = "Abort all running requests")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "Abort process has been successfully initialized.") })
    @ResourceAccess(description = "Abort all running requests", role = DefaultRole.PROJECT_ADMIN)
    public void abortRequests() {
        LOGGER.debug("Received request to abort requests");
        // abortRequests being asynchronous method, we have to give it the tenant
        requestService.abortRequests(runtimeTenantResolver.getTenant());
    }

    @PostMapping(REQUEST_DELETE_PATH)
    @Operation(summary = "Delete selected requests", description = "Delete all requests matching given search criteria")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "Delete process has been successfully initialized.") })
    @ResourceAccess(description = "Delete requests", role = DefaultRole.ADMIN)
    public void delete(@io.swagger.v3.oas.annotations.parameters.RequestBody(description =
                                                                                 "Set of search criteria to find requests"
                                                                                 + " to delete",
                                                                             content = @Content(schema = @Schema(
                                                                                 implementation = SearchRequestParameters.class)))
                       @Parameter(description = "Search criteria for requests to delete") @Valid @RequestBody
                       SearchRequestParameters filters) {
        LOGGER.debug("Received request to delete OAIS entities");
        requestService.scheduleRequestDeletionJob(filters);
    }

    @Override
    public EntityModel<RequestDto> toResource(RequestDto element, Object... extras) {
        EntityModel<RequestDto> resource = resourceService.toResource(element);

        if ((InternalRequestState.ERROR == element.getState()) || (element.getState()
                                                                   == InternalRequestState.ABORTED)) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "retryRequests",
                                    LinkRelation.of("RETRY"),
                                    MethodParamFactory.build(SearchRequestParameters.class));
        }
        if (!Lists.newArrayList(InternalRequestState.RUNNING, InternalRequestState.CREATED)
                  .contains(element.getState())) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "delete",
                                    LinkRels.DELETE,
                                    MethodParamFactory.build(SearchRequestParameters.class));
        }

        return resource;
    }
}
