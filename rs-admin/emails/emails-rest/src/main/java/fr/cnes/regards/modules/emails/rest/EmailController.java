/*
 * LICENSE_PLACEHOLDER
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
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.emails.domain.Email;
import fr.cnes.regards.modules.emails.service.IEmailService;
import fr.cnes.regards.modules.emails.signature.IEmailSignature;

/**
 * Controller defining the REST entry points of the module
 *
 * @author Xavier-Alexandre Brochard
 *
 */
@RestController
@ModuleInfo(name = "emails", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class EmailController implements IEmailSignature {

    /**
     * The service responsible for handling CRUD and mailing operations
     */
    @Autowired
    private IEmailService emailService;

    @Override
    @ResourceAccess(description = "Retrieve all emails", name = "email")
    public ResponseEntity<List<Email>> retrieveEmails() {
        final List<Email> emails = emailService.retrieveEmails();
        return new ResponseEntity<>(emails, HttpStatus.OK);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.emails.signature.IEmailSignature#sendEmail1(org.springframework.mail.SimpleMailMessage)
     */
    @Override
    @ResourceAccess(description = "Send an email to recipients", name = "email")
    public ResponseEntity<SimpleMailMessage> sendEmail(@Valid @RequestBody final SimpleMailMessage pMessage) {
        final SimpleMailMessage created = emailService.sendEmail(pMessage);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Override
    @ResourceAccess(description = "Retrieve an email", name = "email")
    public ResponseEntity<Email> retrieveEmail(@PathVariable("mail_id") final Long pId) {
        final Email email = emailService.retrieveEmail(pId);
        return new ResponseEntity<>(email, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Send again an email", name = "email")
    public void resendEmail(@PathVariable("mail_id") final Long pId) {
        emailService.resendEmail(pId);
    }

    @Override
    @ResourceAccess(description = "Delete an email", name = "email")
    public void deleteEmail(@PathVariable("mail_id") final Long pId) {
        emailService.deleteEmail(pId);
    }

}