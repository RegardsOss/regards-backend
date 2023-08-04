package fr.cnes.regards.modules.notifier.client;

import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;

import java.util.List;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface INotifierClient {

    void sendNotifications(List<NotificationRequestEvent> notification);
}
