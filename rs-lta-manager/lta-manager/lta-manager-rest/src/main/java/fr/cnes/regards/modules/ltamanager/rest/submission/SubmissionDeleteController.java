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
package fr.cnes.regards.modules.ltamanager.rest.submission;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.ltamanager.domain.submission.search.SearchSubmissionRequestParameters;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.service.deletion.SubmissionDeleteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to delete {@link SubmissionRequestDto}
 *
 * @author tguillou
 **/
@Tag(name = "Deletion controller")
@RestController
public class SubmissionDeleteController extends AbstractSubmissionController {

    private final SubmissionDeleteService deleteService;

    protected SubmissionDeleteController(IResourceService resourceService, SubmissionDeleteService deleteService) {
        super(resourceService);
        this.deleteService = deleteService;
    }

    @Operation(summary = "Asynchronously delete a selection of submission request.",
               description = "Find and delete submission requests from criterias defined in request body.")
    @ApiResponses(value = { @ApiResponse(responseCode = "201",
                                         description = "The SubmissionRequest deletion will be take in account. "
                                                       + "This not means deletion is already done, but will be scheduled in the near future."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         content = { @Content(mediaType = "application/html") }),
                            @ApiResponse(responseCode = "422",
                                         description = "The submission request criteria dto syntax is incorrect.",
                                         content = { @Content(mediaType = "application/json") }) })
    @ResourceAccess(description = "Endpoint to delete submission requests that match the criterias in body.",
                    role = DefaultRole.EXPLOIT)
    @DeleteMapping
    public ResponseEntity<String> deleteSubmissionRequests(@io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Set of search criterion.",
        content = @Content(schema = @Schema(implementation = SearchSubmissionRequestParameters.class))) @RequestBody
                                                           @Valid SearchSubmissionRequestParameters searchCriterion)
        throws ModuleException {
        try {
            deleteService.scheduleRequestDeletionJob(searchCriterion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
