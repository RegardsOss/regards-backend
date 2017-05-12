/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.emails.dao.IEmailRepository;
import fr.cnes.regards.modules.emails.domain.Email;

/**
 *
 * Class SimpleEmailService
 *
 * Simple mail service don't persist mail entities in database. To persist entities use EmailService.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Service
public class SimpleEmailService extends AbstractEmailService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleEmailService.class);

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
    public SimpleEmailService(final JavaMailSender pMailSender) {
        super();
        mailSender = pMailSender;
    }

    @Override
    public List<Email> retrieveEmails() {
        // No mail saved in SimpleMailService
        return new ArrayList<>();
    }

    @Override
    public SimpleMailMessage sendEmail(final SimpleMailMessage pEmail) {
        return sendMailWithSender(pEmail);
    }

    @Override
    public Email retrieveEmail(final Long pId) throws ModuleException {
        // Mail are not saved
        return null;
    }

    @Override
    public void resendEmail(final Long pId) throws ModuleException {
        // Mail are not saved
    }

    @Override
    public void deleteEmail(final Long pId) {
        // Mail are not saved
    }

    @Override
    public boolean exists(final Long pId) {
        // Mail are not saved
        return false;
    }

    @Override
    protected JavaMailSender getMailSender() {
        return mailSender;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
