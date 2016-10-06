/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.rest;

import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.modules.core.annotation.ModuleInfo;
import fr.cnes.regards.modules.emails.service.IEmailService;
import fr.cnes.regards.modules.emails.signature.EmailSignature;

/**
 * Controller defining the REST entry points of the module
 *
 * @author xbrochard
 *
 */
@RestController
@ModuleInfo(name = "emails", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
public class EmailController implements EmailSignature {

    /**
     * The service responsible for handling CRUD and mailing operations
     */
    @Autowired
    private IEmailService emailService_;

    @Override
    public HttpEntity<List<MimeMessage>> retrieveEmails() {
        List<MimeMessage> emails = emailService_.retrieveEmails();
        return new ResponseEntity<>(emails, HttpStatus.OK);
    }

    @Override
    public HttpEntity<MimeMessage> sendEmail(final String[] pRecipients, final MimeMessage pEmail)
            throws MessagingException {
        MimeMessage email = emailService_.sendEmail(pRecipients, pEmail);
        return new ResponseEntity<>(email, HttpStatus.CREATED);
    }

    @Override
    public HttpEntity<MimeMessage> retrieveEmail(final Long pId) {
        MimeMessage email = emailService_.retrieveEmail(pId);
        return new ResponseEntity<>(email, HttpStatus.OK);
    }

    @Override
    public void resendEmail(final Long pId) {
        emailService_.resendEmail(pId);
    }

    @Override
    public void deleteEmail(final Long pId) {
        emailService_.deleteEmail(pId);
    }

}