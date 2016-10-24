/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.signature;

import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.modules.emails.domain.Email;

/**
 * REST interface to define the entry points of the module.
 *
 * @author CS SI
 */
@RequestMapping("/emails")
public interface IEmailSignature {

    /**
     * Define the endpoint for retrieving the list of sent emails
     *
     * @return A {@link List} of emails as {@link Email} wrapped in an {@link HttpEntity}
     */
    @GetMapping
    HttpEntity<List<Email>> retrieveEmails();

    // /**
    // * Define the endpoint for sending an email to recipients
    // *
    // * @param pEmail
    // * The email in a simple representation.
    // * @return The sent email as {@link Email} wrapped in an {@link HttpEntity}
    // */
    // @PostMapping
    // @ResponseBody
    // HttpEntity<Email> sendEmail(EmailWithRecipientsDTO pEmail);

    /**
     * Define the endpoint for sending an email to recipients
     *
     * @param pEmail
     *            The email in a simple representation.
     * @return The sent email as {@link Email} wrapped in an {@link HttpEntity}
     */
    @PostMapping
    @ResponseBody
    ResponseEntity<SimpleMailMessage> sendEmail(SimpleMailMessage pMessage);

    /**
     * Define the endpoint for retrieving an email
     *
     * @param pId
     *            The email id
     * @return The email as a {@link Email} wrapped in an {@link HttpEntity}
     */
    @GetMapping("/{mail_id}")
    HttpEntity<Email> retrieveEmail(Long pId);

    /**
     * Define the endpoint for re-sending an email
     *
     * @param pId
     *            The email id
     * @return
     */
    @PutMapping("/{mail_id}")
    void resendEmail(Long pId);

    /**
     * Define the endpoint for deleting an email
     *
     * @param pId
     *            The email id
     * @return
     */
    @DeleteMapping("/{mail_id}")
    void deleteEmail(Long pId);

}
