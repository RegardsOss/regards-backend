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
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionRequestInfoDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.session.SessionInfoGlobalDTO;
import fr.cnes.regards.modules.ltamanager.dto.submission.session.SessionInfoPageDto;
import fr.cnes.regards.modules.ltamanager.service.session.SessionInfoItemized;
import fr.cnes.regards.modules.ltamanager.service.session.SubmissionSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author tguillou
 **/
@Tag(name = "Session controller")
@RestController
@RequestMapping(SubmissionSessionController.SESSION_ROOT_PATH)
public class SubmissionSessionController implements IResourceController<SubmissionRequestInfoDto> {

    public static final String SESSION_ROOT_PATH = "/sessions";

    public static final String SESSION_INFO_MAPPING = "/{session}/info";

    public static final String SESSION_ITEMIZED_INFO_MAPPING = SESSION_INFO_MAPPING + "/details";

    private final SubmissionSessionService sessionService;

    protected final IResourceService resourceService;

    protected SubmissionSessionController(IResourceService resourceService, SubmissionSessionService sessionService) {
        this.resourceService = resourceService;
        this.sessionService = sessionService;
    }

    @Operation(summary = "Calculate global state of a session")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Returns session successfully"),
        @ApiResponse(responseCode = "404", description = "Cannot find session for the user.") })
    @GetMapping(path = SESSION_INFO_MAPPING, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to get session details", role = DefaultRole.EXPLOIT)
    public ResponseEntity<SessionInfoGlobalDTO> getSessionGlobalDetails(@PathVariable String session)
        throws EntityNotFoundException {
        return ResponseEntity.ok(sessionService.getGlobalSessionInfo(session));
    }

    @Operation(summary = "Calculate itemized state of a session")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Returns session successfully"),
        @ApiResponse(responseCode = "404", description = "Cannot find session for the user.") })
    @GetMapping(path = SESSION_ITEMIZED_INFO_MAPPING, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to get session details", role = DefaultRole.EXPLOIT)
    public ResponseEntity<SessionInfoPageDto> getSessionDetails(@PathVariable String session,
                                                                Pageable pageable,
                                                                @Parameter(hidden = true)
                                                                PagedResourcesAssembler<SubmissionRequestInfoDto> assembler)
        throws EntityNotFoundException {
        SessionInfoItemized dto = sessionService.getItemizedSessionInfo(session, pageable);
        return ResponseEntity.ok(toStatusPagedResources(dto, assembler));
    }

    public SessionInfoPageDto toStatusPagedResources(SessionInfoItemized page,
                                                     PagedResourcesAssembler<SubmissionRequestInfoDto> assembler) {
        // to get pagination links, and have status :
        // we need to construct a PageModel custom (SessionInfoPageDto), which have a status attribute.
        return new SessionInfoPageDto<>(page.getGlobalStatus(), assembler.toModel(page));
    }

    @Override
    public EntityModel<SubmissionRequestInfoDto> toResource(SubmissionRequestInfoDto element, Object... extras) {
        return resourceService.toResource(element);
    }
}
