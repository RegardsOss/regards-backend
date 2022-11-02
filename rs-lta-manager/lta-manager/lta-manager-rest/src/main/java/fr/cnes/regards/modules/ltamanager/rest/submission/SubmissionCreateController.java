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

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseStatus;
import fr.cnes.regards.modules.ltamanager.service.submission.creation.SubmissionCreateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Controller to create a {@link SubmissionRequestDto} from a {@link SubmissionRequestDto}
 **/
@Tag(name = "Create controller")
@RestController
public class SubmissionCreateController extends AbstractSubmissionController
    implements IResourceController<SubmissionResponseDto> {

    private static final String REPLACE_PATH = "/replace";

    private final SubmissionCreateService createService;

    protected final IAuthenticationResolver authResolver;

    protected SubmissionCreateController(IResourceService resourceService,
                                         IAuthenticationResolver authResolver,
                                         SubmissionCreateService createService) {
        super(resourceService);
        this.authResolver = authResolver;
        this.createService = createService;
    }

    @Operation(summary = "Register a submission request.",
        description = "Create and save a submission request from a submission request dto.")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description =
        "The SubmissionRequest was successfully saved. Returns "
        + "SubmissionResponseDto with the id of the request."),
        @ApiResponse(responseCode = "403", description = "The endpoint is not accessible for the user.",
            content = { @Content(mediaType = "application/html") }),
        @ApiResponse(responseCode = "422", description = "The submission request dto syntax is incorrect.",
            content = { @Content(mediaType = "application/json") }) })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to create and save a submission request from a submission request dto.",
        role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<SubmissionResponseDto>> createSubmissionRequest(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Submission request dto to be processed.",
            content = @Content(schema = @Schema(implementation = SubmissionRequestDto.class))) @RequestBody
        @Valid SubmissionRequestDto submissionRequestDto) {
        // init owner with the user who is sending the request
        submissionRequestDto.setOwner(StringUtils.truncate(authResolver.getUser(), 128));
        // create and save a submission request with the submission request dto
        SubmissionResponseDto submissionResponse = this.createService.handleSubmissionRequestCreation(
            submissionRequestDto);
        if (submissionResponse.getResponseStatus() == SubmissionResponseStatus.GRANTED) {
            return ResponseEntity.status(HttpStatus.CREATED).body(EntityModel.of(submissionResponse));
        } else {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(EntityModel.of(submissionResponse));
        }
    }

    @Operation(summary = "Register a submission request with replacement if exists.", description =
        "Create and save a submission request from a submission request dto. "
        + "If the product already exists, it will be overridden")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description =
        "The SubmissionRequest was successfully saved. Returns "
        + "SubmissionResponseDto with the id of the request."),
        @ApiResponse(responseCode = "403", description = "The endpoint is not accessible for the user.",
            content = { @Content(mediaType = "application/html") }),
        @ApiResponse(responseCode = "422", description = "The submission request dto syntax is incorrect.",
            content = { @Content(mediaType = "application/json") }) })
    @PostMapping(value = REPLACE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to create and save a submission request from a submission request dto.",
        role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<SubmissionResponseDto>> createSubmissionRequestWithReplace(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Submission request dto to be processed.",
            content = @Content(schema = @Schema(implementation = SubmissionRequestDto.class))) @RequestBody
        @Valid SubmissionRequestDto submissionRequestDto) {
        submissionRequestDto.setReplaceMode(true);
        return createSubmissionRequest(submissionRequestDto);
    }

    @Override
    public EntityModel<SubmissionResponseDto> toResource(SubmissionResponseDto responseDto, Object... extras) {
        return resourceService.toResource(responseDto);
    }
}
