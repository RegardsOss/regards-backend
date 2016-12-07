/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import fr.cnes.regards.modules.notification.domain.Notification;

/**
 * Strategy interface to define a method for sending notifications. It can be mail, ihm...
 *
 * @author Xavier-Alexandre Brochard
 */
@FunctionalInterface
public interface ISendingStrategy {

    /**
     * Send the passed notification
     *
     * @param pNotification
     *            The notification to send
     * @param pRecipients
     *            The list of recipients' emails
     */
    void send(Notification pNotification, String[] pRecipients);
}
