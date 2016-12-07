/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.emails.dao.IEmailRepository;
import fr.cnes.regards.modules.emails.domain.Email;

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
    private final IEmailRepository emailRepository;

    /**
     * Spring Framework interface for sending email
     */
    private final JavaMailSender mailSender;

    /**
     * Creates an {@link EmailService} wired to the given {@link IEmailRepository}.
     *
     * @param pEmailRepository
     *            Autowired by Spring. Must not be {@literal null}.
     * @param pMailSender
     *            Autowired by Spring. Must not be {@literal null}.
     */
    public EmailService(final IEmailRepository pEmailRepository, final JavaMailSender pMailSender) {
        super();
        emailRepository = pEmailRepository;
        mailSender = pMailSender;
    }

    @Override
    public List<Email> retrieveEmails() {
        final List<Email> emails = new ArrayList<>();
        final Iterable<Email> results = emailRepository.findAll();
        if (results != null) {
            results.forEach(emails::add);
        }
        return emails;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.emails.service.IEmailService#sendEmail(org.springframework.mail.SimpleMailMessage)
     */
    @Override
    public SimpleMailMessage sendEmail(final SimpleMailMessage pMessage) {
        // Create the savable DTO
        final Email email = createEmailFromSimpleMailMessage(pMessage);

        // Persist in DB
        emailRepository.save(email);

        // Send the mail
        mailSender.send(pMessage);

        return pMessage;
    }

    @Override
    public Email retrieveEmail(final Long pId) {
        return emailRepository.findOne(pId);
    }

    @Override
    public void resendEmail(final Long pId) {
        final Email email = retrieveEmail(pId);
        final SimpleMailMessage message = createSimpleMailMessageFromEmail(email);
        mailSender.send(message);
    }

    @Override
    public void deleteEmail(final Long pId) {
        emailRepository.delete(pId);
    }

    @Override
    public boolean exists(final Long pId) {
        return emailRepository.exists(pId);
    }

    /**
     * Create a {@link SimpleMailMessage} with same content as the passed {@link Email}.
     *
     * @param pEmail
     *            The email
     * @return The message
     */
    private SimpleMailMessage createSimpleMailMessageFromEmail(final Email pEmail) {
        final SimpleMailMessage message = new SimpleMailMessage();
        message.setBcc(pEmail.getBcc());
        message.setCc(pEmail.getCc());
        message.setFrom(pEmail.getFrom());
        message.setReplyTo(pEmail.getReplyTo());
        message.setSentDate(pEmail.getSentDate());
        message.setSubject(pEmail.getSubject());
        message.setText(pEmail.getText());
        message.setTo(pEmail.getTo());
        return message;
    }

    /**
     * Create a domain {@link Email} with same content as the passed {@link SimpleMailMessage} in order to save it in
     * db.
     *
     * @param pMessage
     *            The message
     * @return The savable email
     */
    private Email createEmailFromSimpleMailMessage(final SimpleMailMessage pMessage) {
        final Email email = new Email();
        email.setBcc(pMessage.getBcc());
        email.setCc(pMessage.getCc());
        email.setFrom(pMessage.getFrom());
        email.setReplyTo(pMessage.getReplyTo());
        email.setSentDate(pMessage.getSentDate());
        email.setSubject(pMessage.getSubject());
        email.setText(pMessage.getText());
        email.setTo(pMessage.getTo());
        return email;
    }

}
