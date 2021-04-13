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
package fr.cnes.regards.modules.featureprovider.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestSearchParameters;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPagedModel;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequest;
import fr.cnes.regards.modules.featureprovider.service.IFeatureExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 *
 * Controller to provide management endpoints about {@link FeatureExtractionRequest}s
 *
 * @author Sébastien Binda
 *
 */
@RestController
@RequestMapping(FeatureExtractionRequestController.ROOT_PATH)
public class FeatureExtractionRequestController implements IResourceController<FeatureRequestDTO> {

    public static final String ROOT_PATH = "/extraction/requests";

    public static final String RETRY_PATH = "/retry";

    public static final String REQUEST_SEARCH_TYPE_PATH = "/search";

    @Autowired
    private IFeatureExtractionService featureExtractionService;

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
            FeatureRequestSearchParameters parameters, Pageable page) {
        FeatureRequestsSelectionDTO selection = FeatureRequestsSelectionDTO.build().withFilters(parameters);
        return new ResponseEntity<>(toResources(featureExtractionService.findRequests(selection, page)), HttpStatus.OK);
    }

    @Operation(summary = "Delete feature requests by selection", description = "Delete feature requests by selection")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Delete feature requests by selection") })
    @RequestMapping(method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Delete feature request by id", role = DefaultRole.EXPLOIT)
    public ResponseEntity<RequestHandledResponse> deleteRequests(
            @Parameter(description = "Requests selection") @Valid @RequestBody FeatureRequestsSelectionDTO selection) {
        return new ResponseEntity<RequestHandledResponse>(featureExtractionService.deleteRequests(selection),
                HttpStatus.OK);
    }

    @Operation(summary = "Retry feature requests by selection", description = "Retry feature requests by selection")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Retry feature requests by selection") })
    @RequestMapping(method = RequestMethod.POST, path = RETRY_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Retry feature requests", role = DefaultRole.EXPLOIT)
    public ResponseEntity<RequestHandledResponse> retryRequests(
            @Parameter(description = "Requests selection") @Valid @RequestBody FeatureRequestsSelectionDTO selection) {
        return new ResponseEntity<RequestHandledResponse>(featureExtractionService.retryRequests(selection),
                HttpStatus.OK);
    }

    /**
     * Format response with HATEOAS
     */
    private RequestsPagedModel<EntityModel<FeatureRequestDTO>> toResources(
            RequestsPage<FeatureRequestDTO> requestsPage) {
        RequestsPagedModel<EntityModel<FeatureRequestDTO>> pagedResource = RequestsPagedModel
                .wrap(requestsPage.getContent(), new PagedModel.PageMetadata(requestsPage.getSize(),
                        requestsPage.getNumber(), requestsPage.getTotalElements(), requestsPage.getTotalPages()),
                      requestsPage.getInfo());
        pagedResource.getContent().forEach(resource -> addLinks(resource));
        return pagedResource;
    }

    private void addLinks(EntityModel<FeatureRequestDTO> resource) {
        // Request are deletable only if not scheduled
        if (!resource.getContent().isProcessing()) {
            resourceService.addLink(resource, this.getClass(), "deleteRequest", LinkRels.DELETE,
                                    MethodParamFactory.build(Long.class, resource.getContent().getId()));
            resourceService.addLink(resource, this.getClass(), "retryRequest", LinkRelation.of("retry"),
                                    MethodParamFactory.build(Long.class, resource.getContent().getId()));
        }
    }

    @Override
    public EntityModel<FeatureRequestDTO> toResource(FeatureRequestDTO element, Object... extras) {
        EntityModel<FeatureRequestDTO> resource = resourceService.toResource(element);
        addLinks(resource);
        return resource;
    }

}
