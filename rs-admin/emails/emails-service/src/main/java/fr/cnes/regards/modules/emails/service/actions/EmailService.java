/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.service.actions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.emails.dao.IEmailRepository;
import fr.cnes.regards.modules.emails.domain.Email;
import fr.cnes.regards.modules.emails.domain.Recipient;

/**
 * {@link IEmailService} implementation
 *
 * @author Xavier-Alexandre Brochard
 *
 */
@Service
public class EmailService implements IEmailService {

    /**
     * CRUD repository managing emails
     */
    @Autowired
    private IEmailRepository emailRepository_;

    @Override
    public Iterable<Email> retrieveEmails() {
        return emailRepository_.findAll();
    }

    @Override
    public Email sendEmail(Iterable<Recipient> pRecipients, Email pEmail) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Email retrieveEmail(Long pId) {
        return emailRepository_.findOne(pId);
    }

    @Override
    public void resendEmail(Long pId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteEmail(Long pId) {
        Email email = emailRepository_.findOne(pId);
        emailRepository_.delete(email);
    }

}
