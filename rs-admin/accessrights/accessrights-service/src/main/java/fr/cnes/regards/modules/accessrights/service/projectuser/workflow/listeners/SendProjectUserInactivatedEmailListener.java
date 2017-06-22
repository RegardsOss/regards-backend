/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.listeners;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnDenyEvent;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnInactiveEvent;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import fr.cnes.regards.modules.templates.service.TemplateServiceConfiguration;

/**
 * Listen to {@link OnDenyEvent} in order to warn the user its account request was refused.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class SendProjectUserInactivatedEmailListener implements ApplicationListener<OnInactiveEvent> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SendProjectUserInactivatedEmailListener.class);

    /**
     * The email template
     */
    private static final String TEMPLATE = TemplateServiceConfiguration.PROJECT_USER_INACTIVATED_TEMPLATE;

    /**
     * Service handling CRUD operations on email templates
     */
    private final ITemplateService templateService;

    /**
     * Client for sending emails
     */
    private final IEmailClient emailClient;

    /**
     * Account service
     */
    private final IAccountService accountService;

    /**
     * @param pTemplateService
     * @param pEmailClient
     * @param pAccountService
     */
    public SendProjectUserInactivatedEmailListener(ITemplateService pTemplateService, IEmailClient pEmailClient,
            IAccountService pAccountService) {
        super();
        templateService = pTemplateService;
        emailClient = pEmailClient;
        accountService = pAccountService;
    }

    /**
     * Send a password reset email based on information stored in the passed event
     *
     * @param pEvent
     *            the init event
     */
    @Override
    public void onApplicationEvent(final OnInactiveEvent pEvent) {
        // Retrieve the user
        ProjectUser projectUser = pEvent.getProjectUser();

        // Build the list of recipients
        String[] recipients = { projectUser.getEmail() };

        // Create a hash map in order to store the data to inject in the mail
        Map<String, String> data = new HashMap<>();
        try {
            data.put("name", accountService.retrieveAccountByEmail(projectUser.getEmail()).getFirstName());
        } catch (EntityNotFoundException e) {
            LOGGER.error("Could not find the associated Account for templating the email content.", e);
            data.put("name", "");
        }

        SimpleMailMessage email;
        try {
            email = templateService.writeToEmail(TEMPLATE, data, recipients);
        } catch (final EntityNotFoundException e) {
            LOGGER.error("Could not find the template to generate the email notifying the account refusal. Falling back to default.",
                         e);
            email = writeToEmailDefault(data, recipients);
        }

        // Send it
        FeignSecurityManager.asSystem();
        emailClient.sendEmail(email);
        FeignSecurityManager.reset();
    }

    /**
     * Send super simple mail in case the template service fails
     *
     * @param data the data
     * @param recipients the recipients
     * @return the result email
     */
    private SimpleMailMessage writeToEmailDefault(Map<String, String> data, String[] recipients) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipients);
        email.setSubject("REGARDS - Access deactivated");
        email.setText("Your access has been deactivated.");
        return email;
    }

}