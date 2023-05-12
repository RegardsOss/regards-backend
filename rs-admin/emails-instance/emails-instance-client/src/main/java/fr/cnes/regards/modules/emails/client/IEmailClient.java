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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * Feign client exposing the emails module endpoints to other microservices plugged through Eureka.
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 */

@RestClient(name = "rs-admin-instance", contextId = "rs-admin-instance.emails-instance-client")
public interface IEmailClient {

    String ROOT_PATH = "/email";

    /**
     * Save a mail to send in database. The email will send by an asynchronous process thanks to
     * a scheduler. <br>Prefer using
     * {@link #sendEmail(String, String, String, String...)}
     *
     * @param mailMessage The mail in a simple representation.
     */
    @PostMapping(path = ROOT_PATH,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> sendEmail(SimpleMailMessage mailMessage);

    /**
     * Helper method to send email without creating SimpleMailMessage before call {@link #sendEmail}.
     *
     * @param message email message
     * @param subject email subject
     * @param from    email sender, if you don't care about who is sending, set null to use default.
     * @param to      recipients
     */
    default ResponseEntity<Void> sendEmail(String message, String subject, @Nullable String from, String... to) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setFrom(from);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        mailMessage.setSentDate(new Date());

        return sendEmail(mailMessage);
    }
}