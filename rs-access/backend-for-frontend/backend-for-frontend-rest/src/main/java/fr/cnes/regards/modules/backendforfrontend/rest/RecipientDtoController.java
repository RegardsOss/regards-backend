/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.backendforfrontend.rest;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.notifier.client.IRecipientClient;
import fr.cnes.regards.modules.notifier.dto.RecipientDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Controller to find all recipients through of rs-notifier
 *
 * @author Stephane Cortine
 */
@RestController
@RequestMapping(RecipientDtoController.RECIPIENTS_ROOT_PATH)
public class RecipientDtoController {

    public static final String RECIPIENTS_ROOT_PATH = "/recipients";

    @Autowired
    private IRecipientClient client;

    @ResourceAccess(description = "Endpoint to retrieve all recipient or only recipients enabling the direct "
                                  + "notification or only "
                                  + "them not enabling the direct notification")
    @GetMapping
    @Operation(summary = "List all recipient",
               description = "List all recipient if missing parameter, or only recipients enabling the "
                             + "direct notification or only them not enabling the direct notification")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "List of recipients") })
    public ResponseEntity<Set<RecipientDto>> findRecipients(
        @Parameter(description = "Recipient enable or not the direct notification") @RequestParam(required = false)
        final Boolean directNotificationEnabled) {
        FeignSecurityManager.asSystem();
        try {
            return client.findRecipients(directNotificationEnabled);
        } finally {
            FeignSecurityManager.reset();
        }
    }

}
