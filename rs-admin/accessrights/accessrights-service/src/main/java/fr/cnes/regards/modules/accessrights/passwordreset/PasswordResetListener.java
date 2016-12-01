/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.passwordreset;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.service.ITemplateService;

/**
 * Listen to {@link OnPasswordResetEvent} in order to send a password reset to the user when required.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class PasswordResetListener implements ApplicationListener<OnPasswordResetEvent> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetListener.class);

    /**
     * The password reset email template code
     */
    private static final String MDP_RESET_TEMPLATE = "passwordResetTemplate";

    /**
     * The password reset service. Autowired by Spring.
     */
    private final IPasswordResetService passwordResetService;

    /**
     * Template Service. Autowired by Spring.
     */
    private final ITemplateService templateService;

    /**
     * Email Client. Autowired by Spring.
     */
    private final IEmailClient emailClient;

    /**
     * @param pPasswordResetService
     *            the password reset service
     * @param pTemplateService
     *            the template service
     * @param pEmailClient
     *            the email client
     */
    public PasswordResetListener(final IPasswordResetService pPasswordResetService,
            final ITemplateService pTemplateService, final IEmailClient pEmailClient) {
        super();
        passwordResetService = pPasswordResetService;
        templateService = pTemplateService;
        emailClient = pEmailClient;
    }

    @Override
    public void onApplicationEvent(final OnPasswordResetEvent pEvent) {
        this.sendPasswordResetEmail(pEvent);
    }

    /**
     * Send a password reset email based on information stored in the passed event
     *
     * @param pEvent
     *            the init event
     */
    private void sendPasswordResetEmail(final OnPasswordResetEvent pEvent) {
        final Account account = pEvent.getAccount();
        final String token = UUID.randomUUID().toString();
        passwordResetService.createPasswordResetToken(account, token);

        // Create the password reset email from a template
        final String[] recipients = { account.getEmail() };
        final String passwordResetUrl = pEvent.getAppUrl() + "/passwordReset/" + token;
        final Map<String, String> data = new HashMap<>();

        data.put("name", account.getFirstName());
        data.put("passwordResetUrl", passwordResetUrl);
        SimpleMailMessage email;
        try {
            email = templateService.writeToEmail(MDP_RESET_TEMPLATE, data, recipients);
        } catch (final EntityNotFoundException e) {
            LOG.warn("Could not find the template to generate a password reset email. Falling back to default.", e);
            email = new SimpleMailMessage();
            email.setTo(recipients);
            email.setSubject("REGARDS - Password Reset");
            email.setText("Please click on the following link to set a new password for your account: "
                    + passwordResetUrl);
        }

        // Send it
        emailClient.sendEmail(email);
    }
}