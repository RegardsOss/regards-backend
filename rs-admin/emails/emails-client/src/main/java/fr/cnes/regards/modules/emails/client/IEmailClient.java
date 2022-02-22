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
package fr.cnes.regards.modules.emails.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.emails.domain.Email;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign client exposing the emails module endpoints to other microservices plugged through Eureka.
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 */

@RestClient(name = "rs-admin", contextId = "rs-admin.emails-client")
public interface IEmailClient {

    String ROOT_PATH = "/emails";

    /**
     * Define the endpoint for retrieving the list of sent emails
     * @return A {@link List} of emails as {@link Email} wrapped in an {@link ResponseEntity}
     */
    @GetMapping(path = ROOT_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Email>> retrieveEmails();

    /**
     * Define the endpoint for sending an email to recipients.<br>Prefer using {@link #sendEmail(String, String, String, String...)}
     * @param pMessage The email in a simple representation.
     */
    @PostMapping(path = ROOT_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> sendEmail(SimpleMailMessage pMessage);

    /**
     * Helper method to send mail without creating SimpleMailMessage before call
     * @param message email message
     * @param subject email subject
     * @param from email sender, if you don't care about who is sending, set null to use default.
     * @param to recipients
     */
    default ResponseEntity<Void> sendEmail(String message, String subject, String from, String... to) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setSubject(subject);
        mail.setFrom(from);
        mail.setText(message);
        mail.setTo(to);
        return sendEmail(mail);
    }

    /**
     * Define the endpoint for retrieving an email
     * @param pId The email id
     * @return The email as a {@link Email} wrapped in an {@link ResponseEntity}
     */
    @GetMapping(path = ROOT_PATH + "/{mail_id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Email> retrieveEmail(Long pId);

    /**
     * Define the endpoint for re-sending an email
     */
    @PutMapping(path =  ROOT_PATH + "/{mail_id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    void resendEmail(Long pId);

    /**
     * Define the endpoint for deleting an email
     * @param pId The email id
     */
    @DeleteMapping(path = ROOT_PATH + "/{mail_id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    void deleteEmail(Long pId);
}