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
package fr.cnes.regards.modules.emails.rest;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.emails.service.EmailRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Controller defining the REST entry points of the module for emails
 *
 * @author Xavier-Alexandre Brochard
 */
@Tag(name = "Email request controller")
@RestController
@RequestMapping(value = EmailRequestController.EMAIL_ROOT_PATH)
public class EmailRequestController {

    public static final String EMAIL_ROOT_PATH = "/email";

    /**
     * Mail service for handling CRUD operations for email and operations to send email
     */
    @Autowired
    private EmailRequestService emailRequestService;

    /**
     * Define the endpoint for sending an email to recipients. Transform a mail message in email
     * request for saving in database.
     *
     * @param mailMessage The email in a simple representation.
     */
    @Operation(summary = "Create an email request in database, then send a email by a asynchronous process.")
    @ApiResponses(value = { @ApiResponse(responseCode = "201",
                                         description = "The email was successfully created in database.") })
    @ResponseBody
    @PostMapping
    @ResourceAccess(description = "Send an email to recipients by a asynchronous process.",
                    role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> sendEmail(@Valid @RequestBody SimpleMailMessage mailMessage) {
        emailRequestService.saveEmailRequest(mailMessage, null, null);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

}