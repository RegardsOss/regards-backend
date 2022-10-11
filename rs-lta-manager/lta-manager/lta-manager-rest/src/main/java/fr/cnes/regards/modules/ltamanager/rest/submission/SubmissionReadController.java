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
package fr.cnes.regards.modules.ltamanager.rest.submission;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.ltamanager.domain.submission.search.SubmissionRequestSearchParameters;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionRequestInfoDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmittedSearchResponseDto;
import fr.cnes.regards.modules.ltamanager.service.submission.reading.SubmissionReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @author Iliana Ghazali
 **/
@Tag(name = "Search controller")
@RestController
public class SubmissionReadController extends AbstractSubmissionController
    implements IResourceController<SubmittedSearchResponseDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionReadController.class);

    public static final String INFO_MAPPING = "/{requestId}/info";

    public static final String SEARCH_MAPPING = "/search";

    private final SubmissionReadService readService;

    protected SubmissionReadController(IResourceService resourceService, SubmissionReadService readService) {
        super(resourceService);
        this.readService = readService;
    }

    @Operation(summary = "Check a submission request status.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns submission response status."),
        @ApiResponse(responseCode = "403", description = "The endpoint is not accessible for the user.",
                     useReturnTypeSchema = true, content = {
            @Content(mediaType = "application/html") }),
        @ApiResponse(responseCode = "404", description = "Associated submission request was not found.",
                     useReturnTypeSchema = true, content = {
            @Content(mediaType = "application/json") }) })
    @GetMapping(path = INFO_MAPPING, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to check the progress of a submission request.", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<SubmissionRequestInfoDto>> checkSubmissionRequestStatus(
        @Parameter(description = "Identifier of the submission request to check.") @PathVariable String requestId) {
        SubmissionRequestInfoDto requestStatusInfo = readService.retrieveRequestStatusInfo(requestId);
        if (requestStatusInfo != null) {
            return ResponseEntity.ok(EntityModel.of(requestStatusInfo));
        } else {
            LOGGER.warn("Submission request with id {} was not found in the database", requestId);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Search for submission requests with criteria")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns submitted requests found.", content = {
            @Content(mediaType = "application/json") }),
        @ApiResponse(responseCode = "403", description = "The endpoint is not accessible for the user.", content = {
            @Content(mediaType = "application/html") }) })
    @PostMapping(path = SEARCH_MAPPING, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to search for submission requests.", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<SubmittedSearchResponseDto>>> findSubmittedRequests(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Set of search criterion.",
                                                              content = @Content(schema = @Schema(
                                                                  implementation = SubmissionRequestSearchParameters.class)))
        @RequestBody @Valid SubmissionRequestSearchParameters searchCriterion,
        @PageableDefault(sort = { "submissionStatus_statusDate", "requestId" }, direction = Sort.Direction.DESC)
        Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<SubmittedSearchResponseDto> assembler) {
        return ResponseEntity.ok(toPagedResources(readService.retrieveSubmittedRequestsByCriteria(searchCriterion,
                                                                                                  pageable),
                                                  assembler));
    }

    @Override
    public EntityModel<SubmittedSearchResponseDto> toResource(SubmittedSearchResponseDto searchResponseDto,
                                                              Object... extras) {
        return resourceService.toResource(searchResponseDto);
    }
}
