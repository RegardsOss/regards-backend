/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.emails.domain.Email;
import fr.cnes.regards.modules.emails.service.EmailService;
import fr.cnes.regards.modules.emails.service.IEmailService;
import fr.cnes.regards.modules.emails.service.SimpleEmailService;

/**
 * Controller defining the REST entry points of the module
 *
 * @author Xavier-Alexandre Brochard
 *
 */
@RestController
@ModuleInfo(name = "emails", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(value = "/emails")
public class EmailController {

    /**
     * The service responsible for handling CRUD and mailing operations
     */
    @Autowired
    private EmailService emailService;

    /**
     * Mail service without persistence of email sent
     */
    @Autowired
    private SimpleEmailService simpleEmailService;

    /**
     * Tenant resolver used to know current tenant.
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Define the endpoint for retrieving the list of sent emails
     *
     * @return A {@link List} of emails as {@link Email} wrapped in an {@link ResponseEntity}
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve all emails")
    public ResponseEntity<List<Email>> retrieveEmails() {
        if (!runtimeTenantResolver.isInstance()) {
            final List<Email> emails = emailService.retrieveEmails();
            return new ResponseEntity<>(emails, HttpStatus.OK);
        }
        // This method is only allowed with tenant.
        return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * Define the endpoint for sending an email to recipients
     *
     * @param pEmail
     *            The email in a simple representation.
     * @return The sent email as {@link Email} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Send an email to recipients")
    public ResponseEntity<Void> sendEmail(@Valid @RequestBody final SimpleMailMessage pMessage) {
        IEmailService service = emailService;
        if (runtimeTenantResolver.isInstance()) {
            service = simpleEmailService;
        }
        service.sendEmail(pMessage);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Define the endpoint for retrieving an email
     *
     * @param pId
     *            The email id
     * @return The email as a {@link Email} wrapped in an {@link ResponseEntity}
     * @throws ModuleException
     *             if email cannot be found
     */
    @RequestMapping(value = "/{mail_id}", method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve an email")
    public ResponseEntity<Email> retrieveEmail(@PathVariable("mail_id") final Long pId) throws ModuleException {
        if (!runtimeTenantResolver.isInstance()) {
            final Email email = emailService.retrieveEmail(pId);
            return new ResponseEntity<>(email, HttpStatus.OK);
        }

        // This method is only allowed with tenant.
        return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * Define the endpoint for re-sending an email
     *
     * @param pId
     *            The email id
     * @return void
     * @throws ModuleException
     *             if email cannot be found
     */
    @RequestMapping(value = "/{mail_id}", method = RequestMethod.PUT)
    @ResourceAccess(description = "Send again an email")
    public void resendEmail(@PathVariable("mail_id") final Long pId) throws ModuleException {
        if (!runtimeTenantResolver.isInstance()) {
            emailService.resendEmail(pId);
        }
    }

    /**
     * Define the endpoint for deleting an email
     *
     * @param pId
     *            The email id
     * @return void
     */
    @RequestMapping(value = "/{mail_id}", method = RequestMethod.DELETE)
    @ResourceAccess(description = "Delete an email")
    public void deleteEmail(@PathVariable("mail_id") final Long pId) {
        if (!runtimeTenantResolver.isInstance()) {
            emailService.deleteEmail(pId);
        }
    }

}