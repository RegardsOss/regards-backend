/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.client;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.emails.domain.Email;

/**
 * Feign client exposing the emails module endpoints to other microservices plugged through Eureka.
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 */

@RestClient(name = "rs-admin")
@RequestMapping(value = "/emails", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IEmailClient {

    /**
     * Define the endpoint for retrieving the list of sent emails
     *
     * @return A {@link List} of emails as {@link Email} wrapped in an {@link ResponseEntity}
     */
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<List<Email>> retrieveEmails();

    /**
     * Define the endpoint for sending an email to recipients
     *
     * @param pEmail
     *            The email in a simple representation.
     * @return The sent email as {@link Email} wrapped in an {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<SimpleMailMessage> sendEmail(SimpleMailMessage pMessage);

    /**
     * Define the endpoint for retrieving an email
     *
     * @param pId
     *            The email id
     * @return The email as a {@link Email} wrapped in an {@link ResponseEntity}
     */
    @RequestMapping(value = "/{mail_id}", method = RequestMethod.GET)
    ResponseEntity<Email> retrieveEmail(Long pId);

    /**
     * Define the endpoint for re-sending an email
     *
     * @param pId
     *            The email id
     * @return void
     */
    @RequestMapping(value = "/{mail_id}", method = RequestMethod.PUT)
    void resendEmail(Long pId);

    /**
     * Define the endpoint for deleting an email
     *
     * @param pId
     *            The email id
     * @return
     */
    @RequestMapping(value = "/{mail_id}", method = RequestMethod.DELETE)
    void deleteEmail(Long pId);
}