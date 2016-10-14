/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.rest;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.naming.OperationNotSupportedException;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.utils.endpoint.annotation.ResourceAccess;
import fr.cnes.regards.modules.core.annotation.ModuleInfo;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;
import fr.cnes.regards.modules.emails.domain.Email;
import fr.cnes.regards.modules.emails.domain.EmailWithRecipientsDTO;
import fr.cnes.regards.modules.emails.domain.Recipient;
import fr.cnes.regards.modules.emails.service.IEmailService;
import fr.cnes.regards.modules.emails.signature.IEmailSignature;

/**
 * Controller defining the REST entry points of the module
 *
 * @author xbrochard
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

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Data Not Found")
    public void dataNotFound() {
    }

    @ExceptionHandler(AlreadyExistingException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    public void dataAlreadyExisting() {
    }

    @ExceptionHandler(OperationNotSupportedException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "operation not supported")
    public void operationNotSupported() {
    }

    @ExceptionHandler(InvalidValueException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "invalid Value")
    public void invalidValue() {
    }

    @Override
    @ResourceAccess(description = "Retrieve all emails", name = "email")
    public HttpEntity<List<Email>> retrieveEmails() {
        final List<Email> emails = emailService.retrieveEmails();
        return new ResponseEntity<>(emails, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Send an email to recipients", name = "email")
    public HttpEntity<Email> sendEmail(@Valid @RequestBody final EmailWithRecipientsDTO pDto) {
        final Set<Recipient> recipients = pDto.getRecipients();
        Email email = pDto.getEmail();
        email = emailService.sendEmail(recipients, email);
        return new ResponseEntity<>(email, HttpStatus.CREATED);
    }

    @Override
    @ResourceAccess(description = "Retrieve an email", name = "email")
    public HttpEntity<Email> retrieveEmail(@PathVariable("mail_id") final Long pId) {
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