/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.signature;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.modules.emails.domain.Email;
import fr.cnes.regards.modules.emails.domain.Recipient;

/**
 * REST interface to define the entry points of the module.
 *
 * @author Xavier-Alexandre Brochard
 */
@RequestMapping("/emails")
@Produces(MediaType.APPLICATION_JSON_VALUE)
@Consumes(MediaType.APPLICATION_JSON_VALUE)
public interface EmailsSignature {

    /**
     * Define the endpoint for retrieving the list of sent emails
     *
     * @return
     */
    @GetMapping("")
    HttpEntity<Iterable<Email>> retrieveEmails();

    /**
     * Define the endpoint for sending an email to recipients
     *
     * @param pRecipients
     *            The list of recipients
     * @param pEmail
     *            The ready-to-send email
     * @return
     */
    @PostMapping("")
    @ResponseBody
    HttpEntity<Email> sendEmail(Iterable<Recipient> pRecipients, Email pEmail);

    /**
     * Define the endpoint for retrieving an email
     *
     * @param pId
     *            The email id
     * @return
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
