/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.emails.dao.IEmailRepository;
import fr.cnes.regards.modules.emails.domain.EmailDTO;

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
    private final IEmailRepository emailRepository_;

    /**
     * Spring Framework interface for sending email
     */
    private final JavaMailSender mailSender_;

    /**
     * Creates an {@link EmailService} wired to the given {@link IEmailRepository}.
     *
     * @param pEmailRepository
     *            Autowired by Spring. Must not be {@literal null}.
     */
    public EmailService(final IEmailRepository pEmailRepository, final JavaMailSender pMailSender) {
        super();
        emailRepository_ = pEmailRepository;
        mailSender_ = pMailSender;
    }

    @Override
    public List<SimpleMailMessage> retrieveEmails() {
        return emailRepository_.findAll().stream().map(emailDTO -> emailDTO.getMail()).collect(Collectors.toList());
    }

    @Override
    public SimpleMailMessage sendEmail(final String[] pRecipients, final SimpleMailMessage pEmail) {
        // Set the recipients
        pEmail.setTo(pRecipients);

        // Send the mail
        mailSender_.send(pEmail);

        // Persist in DB
        EmailDTO dto = new EmailDTO(pEmail);
        emailRepository_.save(dto);

        return pEmail;
    }

    @Override
    public SimpleMailMessage retrieveEmail(final Long pId) {
        return emailRepository_.findOne(pId).getMail();
    }

    @Override
    public void resendEmail(final Long pId) {
        SimpleMailMessage email = retrieveEmail(pId);
        mailSender_.send(email);
    }

    @Override
    public void deleteEmail(final Long pId) {
        emailRepository_.delete(pId);
    }

    @Override
    public boolean exists(final Long pId) {
        return emailRepository_.exists(pId);
    }

}
