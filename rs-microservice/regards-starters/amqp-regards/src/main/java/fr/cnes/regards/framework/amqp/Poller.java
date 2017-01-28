/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import javax.transaction.Transaction;

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.MultitenantAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.RabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.EventUtils;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;

/**
 * {@link Poller} allows to poll an event from a queue. Using {@link Transaction} on caller will cause event to be poll
 * in a safe manner. If transaction fails, the polled event is return to the broker.
 *
 * @author svissier
 * @author Marc Sordi
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
    private final MultitenantAmqpAdmin regardsAmqpAdmin;

    /**
     * bean assisting us to manipulate virtual hosts
     */
    private final IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    public Poller(RabbitTemplate pRabbitTemplate, MultitenantAmqpAdmin pRegardsAmqpAdmin,
            IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin) {
        super();
        rabbitTemplate = pRabbitTemplate;
        // Enable transaction management : if poll is executed in a transaction and transaction fails, message is return
        // to the broker.
        rabbitTemplate.setChannelTransacted(true);
        regardsAmqpAdmin = pRegardsAmqpAdmin;
        rabbitVirtualHostAdmin = pRabbitVirtualHostAdmin;
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
     * @param pWorkerMode
     *            {@link WorkerMode}
     * @param pTarget
     *            {@link Target}
     * @return event in a {@link TenantWrapper}
     */
    public <T> TenantWrapper<T> poll(String pTenant, Class<T> pEvt, WorkerMode pWorkerMode, Target pTarget) {

        rabbitVirtualHostAdmin.addVhost(pTenant);
        final Exchange exchange = regardsAmqpAdmin.declareExchange(pTenant, pEvt, pWorkerMode, pTarget);
        final Queue queue = regardsAmqpAdmin.declareQueue(pTenant, pEvt, pWorkerMode, pTarget);
        regardsAmqpAdmin.declareBinding(pTenant, queue, exchange, pWorkerMode);

        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), RabbitVirtualHostAdmin.getVhostName(pTenant));
        // the CannotCastException should be thrown that mean someone/something tempered with the broker queue
        @SuppressWarnings("unchecked")
        final TenantWrapper<T> evt = (TenantWrapper<T>) rabbitTemplate
                .receiveAndConvert(regardsAmqpAdmin.getQueueName(pEvt, pWorkerMode, pTarget), 0);
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
        return evt;
    }
}