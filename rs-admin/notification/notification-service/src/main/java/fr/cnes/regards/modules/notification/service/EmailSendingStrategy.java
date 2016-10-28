/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import java.util.Date;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.notification.domain.Notification;

/**
 * Implementation of the {@link INotificationSendingStrategy}.<br>
 * Sends the notifications as emails.
 *
 * @author CS SI
 */
@Component
public class EmailSendingStrategy implements ISendingStrategy {

    /**
     * Feign client from module Email
     */
    private final IEmailClient emailClient;

    /**
     * Creates new strategy with passed email client
     *
     * @param pEmailClient
     *            The email feign client
     */
    public EmailSendingStrategy(final IEmailClient pEmailClient) {
        emailClient = pEmailClient;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.service.ISendingStrategy#send(fr.cnes.regards.modules.
     * notification.domain.Notification)
     */
    @Override
    public void send(final Notification pNotification, final String[] pRecipients) {
        // Build the email from the notification and add recipients
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(pNotification.getSender());
        email.setSentDate(new Date());
        email.setSubject(pNotification.getTitle());
        email.setText(pNotification.getMessage());
        email.setTo(pRecipients);

        // Send the email
        emailClient.sendEmail(email);
    }

}
