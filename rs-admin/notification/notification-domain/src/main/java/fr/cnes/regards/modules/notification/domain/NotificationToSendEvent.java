package fr.cnes.regards.modules.notification.domain;

import org.springframework.context.ApplicationEvent;

import fr.cnes.regards.framework.notification.NotificationLevel;

/**
 * Application event aiming to allow the sending process to be triggered when we receive a notification which is "urgent", {@link NotificationLevel#ERROR} or {@link NotificationLevel#FATAL}
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@SuppressWarnings("serial")
public class NotificationToSendEvent extends ApplicationEvent {

    /**
     * The notification
     */
    private final Notification notification;

    /**
     * Constructor setting the parameter as attribute
     * @param notification
     */
    public NotificationToSendEvent(Notification notification) {
        super(notification);
        this.notification = notification;
    }

    /**
     * @return the notification
     */
    public Notification getNotification() {
        return notification;
    }
}
