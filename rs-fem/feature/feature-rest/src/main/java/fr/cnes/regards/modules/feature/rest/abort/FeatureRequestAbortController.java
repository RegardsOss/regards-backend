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
package fr.cnes.regards.modules.feature.rest.abort;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.service.abort.FeatureRequestAbortService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Rest controller to abort running requests. This endpoint is not part of the nominal workflow, it is a temporary
 * workaround to unblock running requests. Only users with high privileges can access this functionality.
 *
 * @author Iliana Ghazali
 **/
@RestController
@RequestMapping(FeatureRequestAbortController.ROOT_PATH)
public class FeatureRequestAbortController implements IResourceController<FeatureRequestDTO> {

    public static final String ROOT_PATH = "/requests";

    public static final String ABORT_PATH = "/{type}/abort";

    private final FeatureRequestAbortService featureRequestAbortService;

    private final IResourceService resourceService;

    public FeatureRequestAbortController(FeatureRequestAbortService featureRequestAbortService,
                                         IResourceService resourceService) {
        this.featureRequestAbortService = featureRequestAbortService;
        this.resourceService = resourceService;
    }

    @Operation(summary = "Abort feature requests by selection",
               description =
                   "Abort feature requests by selection. Synchronous process, so the number of request handled is limited. "
                   + "Information about number of requests handled is returned in the response.)")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Abort feature requests by selection"),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user."),
                            @ApiResponse(responseCode = "422",
                                         description = "The request selection syntax is incorrect.") })
    @PostMapping(path = ABORT_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Abort feature requests", role = DefaultRole.ADMIN)
    public ResponseEntity<RequestHandledResponse> abortRequests(
        @Parameter(description = "Type of requests to abort.") @PathVariable FeatureRequestTypeEnum type,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Set of search criteria.",
                                                              content = @Content(schema = @Schema(implementation = SearchFeatureRequestParameters.class)))
        @Parameter(description = "Requests selection") @Valid @RequestBody SearchFeatureRequestParameters selection) {
        return new ResponseEntity<>(featureRequestAbortService.abortRequests(selection, type), HttpStatus.OK);
    }

    @Override
    public EntityModel<FeatureRequestDTO> toResource(FeatureRequestDTO element, Object... extras) {
        // abort link is added in FeatureRequestController#searchFeatureRequests
        // This link has to be removed when the abort feature will be deleted.
        return resourceService.toResource(element);
    }
}
