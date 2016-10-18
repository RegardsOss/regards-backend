/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.configuration.AmqpConfiguration;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.AddingRabbitMQVhostException;
import fr.cnes.regards.framework.amqp.exception.AddingRabbitMQVhostPermissionException;

/**
 *
 * @author svissier
 *
 */
@Component
public class Poller {

    /**
     * bean provided by spring to receive message from broker
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * bean assisting us to declare elements
     */
    @Autowired
    private AmqpConfiguration amqpConfiguration;

    /**
     *
     * @param pTenant
     *            tenant to poll message for
     * @param pEvt
     *            evt class token
     * @param pAmqpCommunicationMode
     *            communication mode
     * @return received message from the broker
     * @throws AddingRabbitMQVhostException
     *             represent any error that could occur while trying to add the new Vhost
     * @throws AddingRabbitMQVhostPermissionException
     *             represent any error that could occur while adding the permission to the specified vhost
     */
    public <T> TenantWrapper<T> poll(String pTenant, Class<T> pEvt, AmqpCommunicationMode pAmqpCommunicationMode)
            throws AddingRabbitMQVhostException, AddingRabbitMQVhostPermissionException {
        final TenantWrapper<T> evt;
        amqpConfiguration.addVhost(pTenant);
        final Exchange exchange = amqpConfiguration.declareExchange(pEvt.getClass().getName(), pAmqpCommunicationMode,
                                                                    pTenant);
        final Queue queue = amqpConfiguration.declarequeue(pEvt, pAmqpCommunicationMode, pTenant);
        amqpConfiguration.declareBinding(queue, exchange, exchange.getName(), pAmqpCommunicationMode, pTenant);

        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), pTenant);
        evt = (TenantWrapper<T>) rabbitTemplate
                .receiveAndConvert(amqpConfiguration.getQueueName(pEvt, pAmqpCommunicationMode));
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
        return evt;
    }
}
