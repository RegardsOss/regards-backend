/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.registration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

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
 * Listen to {@link OnAcceptAccountEvent} in order to send a validation email to the user when its account was passed to
 * status ACCEPTED.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class EmailValidationListener implements ApplicationListener<OnAcceptAccountEvent> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(EmailValidationListener.class);

    /**
     * The email validation template code
     */
    private static final String EMAIL_VALIDATION_TEMPLATE_CODE = "emailValidationTemplate";

    /**
     * The emailValidationTemplate
     */
    @Resource
    private String emailValidationTemplate;

    /**
     * The account registrationService. Autowired by Spring.
     */
    private final IRegistrationService registrationService;

    /**
     * Template Service. Autowired by Spring.
     */
    private final ITemplateService templateService;

    /**
     * Email Client. Autowired by Spring.
     */
    private final IEmailClient emailClient;

    /**
     * @param pRegistrationService
     *            the registration service
     * @param pTemplateService
     *            the template service
     * @param pEmailClient
     *            the email client
     */
    public EmailValidationListener(final IRegistrationService pRegistrationService,
            final ITemplateService pTemplateService, final IEmailClient pEmailClient) {
        super();
        registrationService = pRegistrationService;
        templateService = pTemplateService;
        emailClient = pEmailClient;
    }

    @PostConstruct
    public void init() {

    }

    @Override
    public void onApplicationEvent(final OnAcceptAccountEvent pEvent) {
        this.sendValidationEmail(pEvent);
    }

    /**
     *
     * @param pEvent
     *            the init event
     * @throws EntityNotFoundException
     */
    private void sendValidationEmail(final OnAcceptAccountEvent pEvent) {
        final Account account = pEvent.getAccount();
        final String token = UUID.randomUUID().toString();
        registrationService.createVerificationToken(account, token);

        // Create the validation email from a template
        final String[] recipients = { account.getEmail() };

        final String confirmationUrl = pEvent.getAppUrl() + "/validateAccount.html?token=" + token;

        final Map<String, String> data = new HashMap<>();

        data.put("name", account.getFirstName());
        data.put("confirmationUrl", confirmationUrl);
        SimpleMailMessage email;
        try {
            email = templateService.writeToEmail(EMAIL_VALIDATION_TEMPLATE_CODE, data, recipients);
        } catch (final EntityNotFoundException e) {
            LOG.warn("Could not find the template for registration confirmation. Falling back to default template.", e);
            email = new SimpleMailMessage();
            email.setTo(recipients);
            email.setSubject("Registration Confirmation");
            email.setText("Please click on the following link to confirm your registration: " + confirmationUrl);
        }

        // Send it
        emailClient.sendEmail(email);
    }
}