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
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

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
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    public <T> TenantWrapper<T> poll(String pTenant, Class<T> pEvt, AmqpCommunicationMode pAmqpCommunicationMode)
            throws RabbitMQVhostException {
        final TenantWrapper<T> evt;
        amqpConfiguration.addVhost(pTenant);
        final Exchange exchange = amqpConfiguration.declareExchange(pEvt.getName(), pAmqpCommunicationMode, pTenant);
        final Queue queue = amqpConfiguration.declareQueue(pEvt, pAmqpCommunicationMode, pTenant);
        amqpConfiguration.declareBinding(queue, exchange, queue.getName(), pAmqpCommunicationMode, pTenant);

        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), pTenant);
        // the CannotCastException should be thrown that mean someone/something tempered with the broker queue
        evt = (TenantWrapper<T>) rabbitTemplate
                .receiveAndConvert(amqpConfiguration.getQueueName(pEvt, pAmqpCommunicationMode));
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
        return evt;
    }
}
