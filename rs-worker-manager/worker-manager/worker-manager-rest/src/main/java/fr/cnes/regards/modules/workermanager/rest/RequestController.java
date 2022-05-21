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
package fr.cnes.regards.modules.workermanager.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.workermanager.domain.database.LightRequest;
import fr.cnes.regards.modules.workermanager.domain.request.SearchRequestParameters;
import fr.cnes.regards.modules.workermanager.service.requests.RequestService;
import fr.cnes.regards.modules.workermanager.service.requests.scan.RequestScanTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
 * This controller manages Worker requests.
 *
 * @author Théo Lasserre
 */
@Tag(name = "Request controller")
@RestController
@RequestMapping(RequestController.TYPE_MAPPING)
public class RequestController implements IResourceController<LightRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestController.class);

    public static final String TYPE_MAPPING = "/requests";

    public static final String REQUEST_ID_PATH = "/{requestId}";

    /**
     * Controller path to retry requests matching criteria
     */
    public static final String REQUEST_RETRY_PATH = "/retry";

    /**
     * Controller path to delete requests matching criteria
     */
    public static final String REQUEST_DELETE_PATH = "/delete";

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private RequestService requestService;

    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Retrieve a page of requests matching given filters", role = DefaultRole.EXPLOIT)
    @Operation(summary = "Retrieve Requests", description = "Retrieve Requests matching given filters.")
    public ResponseEntity<PagedModel<EntityModel<LightRequest>>> retrieveLightRequestList(
        @Parameter(description = "Filter requests using criteria") @RequestBody SearchRequestParameters filters,
        @Parameter(description = "Sorting and page configuration")
        @PageableDefault(sort = "requestId", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<LightRequest> assembler) {
        Page<LightRequest> requests = requestService.searchLightRequests(filters, pageable);
        return new ResponseEntity<>(toPagedResources(requests, assembler), HttpStatus.OK);
    }

    @RequestMapping(path = REQUEST_ID_PATH, method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve a request matching given requestId", role = DefaultRole.EXPLOIT)
    @Operation(summary = "Retrieve Request", description = "Retrieve a Request by its id.")
    public ResponseEntity<EntityModel<LightRequest>> retrieveLightRequest(
        @Parameter(description = "Request ID", example = "1") @PathVariable("requestId") String requestId)
        throws EntityNotFoundException {
        LightRequest request = requestService.retrieveLightRequest(requestId);
        return new ResponseEntity<>(toResource(request), HttpStatus.OK);
    }

    @RequestMapping(value = REQUEST_RETRY_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "Retry requests matching provided filters", role = DefaultRole.EXPLOIT)
    @Operation(summary = "Retry Requests", description = "Retry Requests matching provided filters.")
    public void retryRequests(@Parameter(description = "Filter requests using criteria") @Valid @RequestBody
                              SearchRequestParameters filters) {
        LOGGER.debug("Received request to retry requests");
        requestService.scheduleRequestRetryJob(filters);
    }

    @RequestMapping(value = REQUEST_DELETE_PATH, method = RequestMethod.DELETE)
    @ResourceAccess(description = "Delete requests matching provided filters", role = DefaultRole.ADMIN)
    @Operation(summary = "Delete Requests", description = "Delete Requests matching provided filters.")
    public void deleteRequests(@Parameter(description = "Filter requests using criteria") @Valid @RequestBody
                               SearchRequestParameters filters) {
        LOGGER.debug("Received request to delete requests");
        requestService.scheduleRequestDeletionJob(filters);
    }

    @Override
    public EntityModel<LightRequest> toResource(LightRequest element, Object... extras) {
        EntityModel<LightRequest> resource = resourceService.toResource(element);
        resourceService.addLink(resource,
                                RequestController.class,
                                "retrieveLightRequest",
                                LinkRels.SELF,
                                MethodParamFactory.build(String.class, element.getRequestId()));
        resourceService.addLink(resource,
                                RequestController.class,
                                "retrieveLightRequest",
                                LinkRels.LIST,
                                MethodParamFactory.build(String.class, element.getRequestId()));
        if (RequestScanTask.BLOCKED_REQUESTS_STATUSES.contains(element.getStatus())) {
            resourceService.addLink(resource,
                                    RequestController.class,
                                    "retryRequests",
                                    LinkRelation.of("retry"),
                                    MethodParamFactory.build(SearchRequestParameters.class));
            resourceService.addLink(resource,
                                    RequestController.class,
                                    "deleteRequests",
                                    LinkRels.DELETE,
                                    MethodParamFactory.build(SearchRequestParameters.class));
        }
        return resource;
    }
}
