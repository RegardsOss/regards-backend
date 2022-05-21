package fr.cnes.regards.modules.notifier.client;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class NotifierClient implements INotifierClient {

    @Autowired
    public IPublisher publisher;

    @Override
    public void sendNotifications(List<NotificationRequestEvent> notifications) {
        publisher.publish(notifications);
    }

}
