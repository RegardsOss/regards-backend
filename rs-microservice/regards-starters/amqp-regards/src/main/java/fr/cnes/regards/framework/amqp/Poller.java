/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.EventUtils;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.utils.IRabbitVirtualHostUtils;
import fr.cnes.regards.framework.amqp.utils.RabbitVirtualHostUtils;

/**
 *
 * @author svissier
 *
 */
public class Poller implements IPoller {

    /**
     * bean provided by spring to receive message from broker
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * bean assisting us to declare elements
     */
    private final RegardsAmqpAdmin regardsAmqpAdmin;

    /**
     * bean assisting us to manipulate virtual hosts
     */
    private final IRabbitVirtualHostUtils rabbitVirtualHostUtils;

    public Poller(RabbitTemplate pRabbitTemplate, RegardsAmqpAdmin pRegardsAmqpAdmin,
            IRabbitVirtualHostUtils pRabbitVirtualHostUtils) {
        super();
        rabbitTemplate = pRabbitTemplate;
        regardsAmqpAdmin = pRegardsAmqpAdmin;
        rabbitVirtualHostUtils = pRabbitVirtualHostUtils;
    }

    @Override
    public <T extends IPollable> TenantWrapper<T> poll(String pTenant, Class<T> pEvent) {
        return poll(pTenant, pEvent, EventUtils.getCommunicationMode(pEvent),
                    EventUtils.getCommunicationTarget(pEvent));
    }

    /**
     * Poll an event
     *
     * @param <T>
     *            event object
     * @param pTenant
     *            tenant
     * @param pEvt
     *            event to poll
     * @param pAmqpCommunicationMode
     *            {@link AmqpCommunicationMode}
     * @param pAmqpCommunicationTarget
     *            {@link AmqpCommunicationTarget}
     * @return event in a {@link TenantWrapper}
     */
    public <T> TenantWrapper<T> poll(String pTenant, Class<T> pEvt, AmqpCommunicationMode pAmqpCommunicationMode,
            AmqpCommunicationTarget pAmqpCommunicationTarget) {

        rabbitVirtualHostUtils.addVhost(pTenant);
        final Exchange exchange = regardsAmqpAdmin.declareExchange(pEvt, pAmqpCommunicationMode, pTenant,
                                                                   pAmqpCommunicationTarget);
        final Queue queue = regardsAmqpAdmin.declareQueue(pEvt, pAmqpCommunicationMode, pTenant,
                                                          pAmqpCommunicationTarget);
        regardsAmqpAdmin.declareBinding(queue, exchange, pAmqpCommunicationMode, pTenant);

        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), RabbitVirtualHostUtils.getVhostName(pTenant));
        // the CannotCastException should be thrown that mean someone/something tempered with the broker queue
        @SuppressWarnings("unchecked")
        final TenantWrapper<T> evt = (TenantWrapper<T>) rabbitTemplate.receiveAndConvert(regardsAmqpAdmin
                .getQueueName(pEvt, pAmqpCommunicationMode, pAmqpCommunicationTarget), 0);
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
        return evt;
    }
}