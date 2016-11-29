/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.registration;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class RegistrationListener implements ApplicationListener<OnRegistrationCompleteEvent> {

    /**
     * The account service. Autowired by Spring.
     */
    @Autowired
    private IRegistrationService service;

    /**
     * Strategy interface for resolving messages
     */
    @Autowired
    private MessageSource messages;

    // /**
    // * Mail sender
    // */
    // @Autowired
    // private JavaMailSender mailSender;

    @Override
    public void onApplicationEvent(final OnRegistrationCompleteEvent pEvent) {
        this.confirmRegistration(pEvent);
    }

    /**
     *
     * @param pEvent
     *            the init event
     */
    private void confirmRegistration(final OnRegistrationCompleteEvent pEvent) {
        final Account user = pEvent.getAccount();
        final String token = UUID.randomUUID().toString();
        service.createVerificationToken(user, token);

        final String recipientAddress = user.getEmail();
        final String subject = "Registration Confirmation";
        final String confirmationUrl = pEvent.getAppUrl() + "/regitrationConfirm.html?token=" + token;
        final String message = messages.getMessage("message.regSucc", null, pEvent.getLocale());

        // // TODO Use the template module
        // final SimpleMailMessage email = new SimpleMailMessage();
        // email.setTo(recipientAddress);
        // email.setSubject(subject);
        // email.setText(message + " rn" + "http://localhost:8080" + confirmationUrl);
        // mailSender.send(email);
    }
}