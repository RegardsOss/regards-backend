/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.utils.IRabbitVirtualHostUtils;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 * @author svissier
 *
 */
public class Publisher {

    /**
     * bean allowing us to send message to the broker
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * configuration initializing required bean
     */
    private final RegardsAmqpAdmin regardsAmqpAdmin;

    /**
     * bean assisting us to manipulate virtual host
     */
    private final IRabbitVirtualHostUtils rabbitVirtualHostUtils;

    public Publisher(RabbitTemplate pRabbitTemplate, RegardsAmqpAdmin pRegardsAmqpAdmin,
            IRabbitVirtualHostUtils pRabbitVirtualHostUtils) {
        super();
        rabbitTemplate = pRabbitTemplate;
        regardsAmqpAdmin = pRegardsAmqpAdmin;
        rabbitVirtualHostUtils = pRabbitVirtualHostUtils;
    }

    /**
     * @param <T>
     *            event to be published
     * @param pEvt
     *            the event you want to publish
     * @param pPriority
     *            priority given to the event
     * @param pAmqpCommunicationMode
     *            publishing mode
     * @param pAmqpCommunicationTarget
     *            publishing scope
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    public final <T> void publish(T pEvt, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget, int pPriority) throws RabbitMQVhostException {
        final String tenant = ((JWTAuthentication) SecurityContextHolder.getContext().getAuthentication())
                .getPrincipal().getTenant();
        final String evtName = pEvt.getClass().getName();
        // add the Vhost corresponding to this tenant
        rabbitVirtualHostUtils.addVhost(tenant);
        final Exchange exchange = regardsAmqpAdmin.declareExchange(evtName, pAmqpCommunicationMode, tenant,
                                                                   pAmqpCommunicationTarget);
        if (pAmqpCommunicationMode.equals(AmqpCommunicationMode.ONE_TO_ONE)) {
            final Queue queue = regardsAmqpAdmin.declareQueue(pEvt.getClass(), AmqpCommunicationMode.ONE_TO_ONE, tenant,
                                                              pAmqpCommunicationTarget);
            regardsAmqpAdmin.declareBinding(queue, exchange, pAmqpCommunicationMode, tenant);
        }
        final TenantWrapper<T> messageSended = new TenantWrapper<>(pEvt, tenant);
        // bind the connection to the right vHost ie tenant to publish the message
        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), tenant);
        // routing key is unnecessary for fanout exchanges but is for direct exchanges
        rabbitTemplate.convertAndSend(exchange.getName(), evtName, messageSended, pMessage -> {
            final MessageProperties propertiesWithPriority = pMessage.getMessageProperties();
            propertiesWithPriority.setPriority(pPriority);
            return new Message(pMessage.getBody(), propertiesWithPriority);
        });
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
    }

    public final <T> void publish(T pEvt, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget) throws RabbitMQVhostException {
        publish(pEvt, pAmqpCommunicationMode, pAmqpCommunicationTarget, 0);
    }

}
