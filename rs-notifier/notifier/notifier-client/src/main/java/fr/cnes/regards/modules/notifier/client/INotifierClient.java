package fr.cnes.regards.modules.notifier.client;

import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;

import java.util.List;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface INotifierClient {

    void sendNotifications(List<NotificationRequestEvent> notification);
}
