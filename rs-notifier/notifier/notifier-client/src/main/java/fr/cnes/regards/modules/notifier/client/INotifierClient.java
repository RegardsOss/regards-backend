package fr.cnes.regards.modules.notifier.client;

import java.util.List;

import fr.cnes.regards.modules.notifier.dto.in.NotificationActionEvent;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface INotifierClient {

    void sendNotifications(List<NotificationActionEvent> notification);
}
