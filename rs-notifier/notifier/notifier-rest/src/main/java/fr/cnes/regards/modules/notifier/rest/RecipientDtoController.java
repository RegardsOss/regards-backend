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
package fr.cnes.regards.modules.notifier.rest;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.notifier.dto.RecipientDto;
import fr.cnes.regards.modules.notifier.service.IRecipientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * REST interface for managing data {@link RecipientDto}(recipient from {@link fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration})
 *
 * @author Stephane Cortine
 */
@RestController
@RequestMapping(RecipientDtoController.RECIPIENTS_ROOT_PATH)
public class RecipientDtoController {

    public static final String RECIPIENTS_ROOT_PATH = "/recipients";

    private final IRecipientService recipientService;

    public RecipientDtoController(IRecipientService recipientService) {
        this.recipientService = recipientService;
    }

    @ResourceAccess(description = "Endpoint to retrieve all recipient or only recipients enabling the direct "
                                  + "notification or only "
                                  + "them not enabling the direct notification", role = DefaultRole.EXPLOIT)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all recipient",
               description = "List all recipient if missing parameter, or only recipients enabling the "
                             + "direct notification or only them not enabling the direct notification")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "List of recipients") })
    public ResponseEntity<Set<RecipientDto>> findRecipients(
        @Parameter(description = "Recipient enable or not the direct notification") @RequestParam(required = false)
        final Boolean directNotificationEnabled) {
        return ResponseEntity.ok(recipientService.findRecipients(directNotificationEnabled));
    }

}
