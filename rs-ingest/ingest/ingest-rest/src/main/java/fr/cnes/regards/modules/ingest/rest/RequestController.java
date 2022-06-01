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
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.sip.VersioningMode;
import fr.cnes.regards.modules.ingest.dto.request.ChooseVersioningRequestParameters;
import fr.cnes.regards.modules.ingest.dto.request.RequestDto;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * This controller manages Requests.
 *
 * @author Léo Mieulet
 */
@RestController
@RequestMapping(RequestController.TYPE_MAPPING)
public class RequestController implements IResourceController<RequestDto> {

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

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IRequestService requestService;

    /**
     * {@link IResourceService} instance
     */
    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IIngestRequestService ingestRequestService;

    /**
     * Retrieve a page of ingest requests according to the given filters
     *
     * @param filters   request filters
     * @param pageable
     * @param assembler
     * @return page of aip metadata respecting the constraints
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Return a page of Requests", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<RequestDto>>> searchRequest(
        @RequestBody SearchRequestsParameters filters,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<RequestDto> assembler) throws ModuleException {
        Page<RequestDto> requests = requestService.findRequestDtos(filters, pageable);
        return new ResponseEntity<>(toPagedResources(requests, assembler), HttpStatus.OK);
    }

    @RequestMapping(value = REQUEST_RETRY_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "Retry requests matching provided filters", role = DefaultRole.EXPLOIT)
    public void retryRequests(@Valid @RequestBody SearchRequestsParameters filters) {
        LOGGER.debug("Received request to retry requests");
        requestService.scheduleRequestRetryJob(filters);
    }

    @RequestMapping(value = VERSIONING_CHOICE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "choose versioning mode for requests matching provided filters",
        role = DefaultRole.EXPLOIT)
    public ResponseEntity<Object> chooseVersioning(@Valid @RequestBody ChooseVersioningRequestParameters filters) {
        if (filters.getNewVersioningMode() == VersioningMode.MANUAL) {
            return ResponseEntity.unprocessableEntity()
                                 .body("You cannot choose " + VersioningMode.MANUAL + " versioning mode!");
        }
        LOGGER.debug("Received request to retry requests");
        ingestRequestService.scheduleRequestWithVersioningMode(filters);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = REQUEST_ABORT_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Retry requests matching provided filters", role = DefaultRole.ADMIN)
    public void abortRequests() {
        LOGGER.debug("Received request to abort requests");
        // abortRequests being asynchronous method, we have to give it the tenant
        requestService.abortRequests(runtimeTenantResolver.getTenant());
    }

    @ResourceAccess(description = "Delete requests", role = DefaultRole.ADMIN)
    @RequestMapping(value = REQUEST_DELETE_PATH, method = RequestMethod.POST)
    public void delete(@Valid @RequestBody SearchRequestsParameters filters) {
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
                                    MethodParamFactory.build(SearchRequestsParameters.class));
        }
        if (!Lists.newArrayList(InternalRequestState.RUNNING, InternalRequestState.CREATED)
                  .contains(element.getState())) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "delete",
                                    LinkRels.DELETE,
                                    MethodParamFactory.build(SearchRequestsParameters.class));
        }

        return resource;
    }
}
