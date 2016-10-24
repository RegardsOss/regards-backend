/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private IEmailClient emailClient;

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
        email.setSubject("Subject");
        email.setText(pNotification.getMessage());
        email.setTo(pRecipients);

        // Send the email
        emailClient.sendEmail(email);
    }

}
