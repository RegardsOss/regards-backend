package fr.cnes.regards.modules.notification.domain;

import org.springframework.context.ApplicationEvent;

/**
 * Application event aiming to allow the sending process to be triggered when we receive a notification which is "urgent", {@link NotificationType#ERROR} or {@link NotificationType#FATAL}
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class NotificationToSendEvent extends ApplicationEvent {

    private final Notification notification;

    public NotificationToSendEvent(Notification notification) {
        super(notification);
        this.notification = notification;
    }

    public Notification getNotification() {
        return notification;
    }
}
