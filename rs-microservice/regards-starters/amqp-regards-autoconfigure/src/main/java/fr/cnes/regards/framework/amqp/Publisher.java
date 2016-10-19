/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.configuration.AmqpConfiguration;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.AddingRabbitMQVhostException;
import fr.cnes.regards.framework.amqp.exception.AddingRabbitMQVhostPermissionException;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 * @author svissier
 *
 */
@Component
public class Publisher {

    /**
     * bean allowing us to send message to the broker
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * configuration initializing required bean
     */
    @Autowired
    private AmqpConfiguration amqpConfiguration;

    /**
     *
     * @param pEvt
     *            the event you want to publish
     * @param pAmqpCommunicationMode
     *            publishing mode
     * @throws RabbitMQVhostException
     * @throws AddingRabbitMQVhostException
     *             represent any error that could occur while trying to add the new Vhost
     * @throws AddingRabbitMQVhostPermissionException
     *             represent any error that could occur while adding the permission to the specified vhost
     */
    public final void publish(Object pEvt, AmqpCommunicationMode pAmqpCommunicationMode, int priority)
            throws RabbitMQVhostException {
        final String tenant = ((JWTAuthentication) SecurityContextHolder.getContext().getAuthentication())
                .getPrincipal().getTenant();
        final String evtName = pEvt.getClass().getName();
        // add the Vhost corresponding to this tenant
        amqpConfiguration.addVhost(tenant);
        final Exchange exchange = amqpConfiguration.declareExchange(evtName, pAmqpCommunicationMode, tenant);
        final TenantWrapper messageSended = new TenantWrapper<>(pEvt, tenant);
        // bind the connection to the right vHost ie tenant to publish the message
        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), tenant);
        // routing key is unnecessary for fanout exchanges but is for direct exchanges
        rabbitTemplate.convertAndSend(exchange.getName(), evtName, messageSended, pMessage -> {
            MessageProperties propertiesWithPriority = pMessage.getMessageProperties();
            propertiesWithPriority.setPriority(priority);
            return new Message(pMessage.getBody(), propertiesWithPriority);
        });
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
    }

    public final void publish(Object pEvt, AmqpCommunicationMode pAmqpCommunicationMode) throws RabbitMQVhostException {
        publish(pEvt, pAmqpCommunicationMode, 0);
    }

}
