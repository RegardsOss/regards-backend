/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.signature;

import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * REST interface to define the entry points of the module.
 *
 * @author Xavier-Alexandre Brochard
 */
@Produces(MediaType.APPLICATION_JSON_VALUE)
@Consumes(MediaType.APPLICATION_JSON_VALUE)
public interface EmailSignature {

    /**
     * Define the endpoint for retrieving the list of sent emails
     *
     * @return A {@link List} of emails as {@link MimeMessage} wrapped in an {@link HttpEntity}
     */
    @GetMapping("/emails")
    HttpEntity<List<MimeMessage>> retrieveEmails();

    /**
     * Define the endpoint for sending an email to recipients
     *
     * @param pRecipients
     *            The list of recipients
     * @param pEmail
     *            The ready-to-send email
     * @return The sent email as {@link MimeMessage} wrapped in an {@link HttpEntity}
     * @throws MessagingException
     */
    @PostMapping("/emails")
    @ResponseBody
    HttpEntity<MimeMessage> sendEmail(String[] pRecipients, MimeMessage pEmail) throws MessagingException;

    /**
     * Define the endpoint for retrieving an email
     *
     * @param pId
     *            The email id
     * @return The email as a {@link MimeMessage} wrapped in an {@link HttpEntity}
     */
    @GetMapping("/emails/{mail_id}")
    HttpEntity<MimeMessage> retrieveEmail(Long pId);

    /**
     * Define the endpoint for re-sending an email
     *
     * @param pId
     *            The email id
     * @return
     */
    @PutMapping("/emails/{mail_id}")
    void resendEmail(Long pId);

    /**
     * Define the endpoint for deleting an email
     *
     * @param pId
     *            The email id
     * @return
     */
    @DeleteMapping("/emails/{mail_id}")
    void deleteEmail(Long pId);

}
