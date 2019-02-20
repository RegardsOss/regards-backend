package fr.cnes.regards.framework.amqp.configuration;

import java.util.HashSet;
import java.util.Set;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.util.MimeTypeUtils;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.notification.NotificationDtoBuilder;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * AMQP listenner advice that notify project_admin that an issue happened while republishing the message.
 * @author Sylvain Vissiere-Guerinet
 */
public class NotifyNRepublishMessageRecoverer extends RepublishMessageRecoverer {

    private final IInstancePublisher publisher;

    private final String microserviceName;

    private final IRabbitVirtualHostAdmin rabbitVhostAdmin;

    public NotifyNRepublishMessageRecoverer(AmqpTemplate errorTemplate, String errorExchange, String errorRoutingKey,
            IInstancePublisher publisher, String microserviceName, IRabbitVirtualHostAdmin rabbitVhostAdmin) {
        super(errorTemplate, errorExchange, errorRoutingKey);
        this.publisher = publisher;
        this.microserviceName = microserviceName;
        this.rabbitVhostAdmin = rabbitVhostAdmin;
    }

    /**
     * Recover by sending a notification message to a specific queue in addition to sending the message to DLQ.
     */
    @Override
    public void recover(Message message, Throwable cause) {
        // Message#toString is already handling encoding and content type if possible
        NotificationDtoBuilder notifBuilder = new NotificationDtoBuilder(message.toString(),
                                                                         "AMQP event has been routed to instance DLQ",
                                                                         NotificationLevel.ERROR,
                                                                         microserviceName);
        notifBuilder.withMimeType(MimeTypeUtils.TEXT_PLAIN);
        Set<String> roles = new HashSet<>();
        roles.add(DefaultRole.PROJECT_ADMIN.name());
        publisher.publish(NotificationEvent.build(notifBuilder.toRoles(roles)));
        // As spring code is not aware of our little trick for multitenancy,
        // lets wrap the call to bind rabbit template to the right vHost
        try {
            rabbitVhostAdmin.bind(AmqpConstants.AMQP_INSTANCE_MANAGER);
            super.recover(message, cause);
        } finally {
            rabbitVhostAdmin.unbind();
        }
    }
}