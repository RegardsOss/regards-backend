/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import javax.transaction.Transaction;

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
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
    private final RegardsAmqpAdmin regardsAmqpAdmin;

    /**
     * bean assisting us to manipulate virtual hosts
     */
    private final IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    public Poller(RabbitTemplate pRabbitTemplate, RegardsAmqpAdmin pRegardsAmqpAdmin,
            IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin) {
        super();
        rabbitTemplate = pRabbitTemplate;
        regardsAmqpAdmin = pRegardsAmqpAdmin;
        rabbitVirtualHostAdmin = pRabbitVirtualHostAdmin;
    }

    @Override
    public <T extends IPollable> TenantWrapper<T> poll(String pTenant, Class<T> pEvent) {
        return poll(pTenant, pEvent, WorkerMode.SINGLE, EventUtils.getCommunicationTarget(pEvent));
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
    @SuppressWarnings("unchecked")
    public <T> TenantWrapper<T> poll(String pTenant, Class<T> pEvt, WorkerMode pWorkerMode, Target pTarget) {

        rabbitVirtualHostAdmin.addVhost(pTenant);

        final Exchange exchange = regardsAmqpAdmin.declareExchange(pTenant, pEvt, pWorkerMode, pTarget);
        final Queue queue = regardsAmqpAdmin.declareQueue(pTenant, pEvt, pWorkerMode, pTarget);
        regardsAmqpAdmin.declareBinding(pTenant, queue, exchange, pWorkerMode);

        return (TenantWrapper<T>) rabbitTemplate
                .receiveAndConvert(regardsAmqpAdmin.getQueueName(pEvt, pWorkerMode, pTarget), 0);

    }

    @Override
    public void bind(String pTenant) {
        // Bind the connection to the right vHost (i.e. tenant to publish the message)
        regardsAmqpAdmin.bind(pTenant);
    }

    @Override
    public void unbind() {
        // Unbind resource
        regardsAmqpAdmin.unbind();
    }
}