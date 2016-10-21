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

import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.utils.RabbitVirtualHostUtils;

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
    private RegardsAmqpAdmin regardsAmqpAdmin;

    /**
     * bean assisting us to manipulate virtual hosts
     */
    @Autowired
    private RabbitVirtualHostUtils rabbitVirtualHostUtils;

    /**
     *
     * @param <T>
     *            event published
     * @param pTenant
     *            tenant to poll message for
     * @param pEvt
     *            event class token
     * @param pAmqpCommunicationMode
     *            communication mode
     * @param pAmqpCommunicationTarget
     *            communication target
     * @return received message from the broker
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */

    public <T> TenantWrapper<T> poll(String pTenant, Class<T> pEvt, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget) throws RabbitMQVhostException {

        rabbitVirtualHostUtils.addVhost(pTenant);
        final Exchange exchange = regardsAmqpAdmin.declareExchange(pEvt.getName(), pAmqpCommunicationMode, pTenant,
                                                                   pAmqpCommunicationTarget);
        final Queue queue = regardsAmqpAdmin.declareQueue(pEvt, pAmqpCommunicationMode, pTenant,
                                                          pAmqpCommunicationTarget);
        regardsAmqpAdmin.declareBinding(queue, exchange, pAmqpCommunicationMode, pTenant);

        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), pTenant);
        // the CannotCastException should be thrown that mean someone/something tempered with the broker queue
        @SuppressWarnings("unchecked")
        final TenantWrapper<T> evt = (TenantWrapper<T>) rabbitTemplate.receiveAndConvert(regardsAmqpAdmin
                .getQueueName(pEvt, pAmqpCommunicationMode, pAmqpCommunicationTarget), 0);
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
        return evt;
    }
}