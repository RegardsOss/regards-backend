/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * {@link Poller} allows to poll an event from a queue. Using transaction on caller will cause event to be poll in a
 * safe manner. If transaction fails, the polled event is return to the broker.
 *
 * @author svissier
 * @author Marc Sordi
 *
 */
public class Poller implements IPoller {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Poller.class);

    /**
     * bean provided by spring to receive message from broker
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * bean assisting us to declare elements
     */
    private final RegardsAmqpAdmin regardsAmqpAdmin;

    /**
     * Resolve thread tenant
     */
    private final IRuntimeTenantResolver threadTenantResolver;

    /**
     * Virtual host admin
     */
    private final IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    public Poller(IRabbitVirtualHostAdmin pVirtualHostAdmin, RabbitTemplate pRabbitTemplate,
            RegardsAmqpAdmin pRegardsAmqpAdmin, IRuntimeTenantResolver pThreadTenantResolver) {
        super();
        this.rabbitVirtualHostAdmin = pVirtualHostAdmin;
        rabbitTemplate = pRabbitTemplate;
        regardsAmqpAdmin = pRegardsAmqpAdmin;
        this.threadTenantResolver = pThreadTenantResolver;
    }

    @Override
    public <T extends IPollable> TenantWrapper<T> poll(Class<T> pEvent) {
        return poll(threadTenantResolver.getTenant(), pEvent, WorkerMode.SINGLE,
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
    @SuppressWarnings("unchecked")
    public <T> TenantWrapper<T> poll(String pTenant, Class<T> pEvt, WorkerMode pWorkerMode, Target pTarget) {

        LOGGER.debug("Polling event {} for tenant {} (Target : {}, WorkerMode : {} )", pEvt.getName(), pTenant, pTarget,
                     pWorkerMode);

        try {
            // Bind the connection to the right vHost (i.e. tenant to publish the message)
            rabbitVirtualHostAdmin.bind(pTenant);

            final Exchange exchange = regardsAmqpAdmin.declareExchange(pTenant, pEvt, pWorkerMode, pTarget);
            final Queue queue = regardsAmqpAdmin.declareQueue(pTenant, pEvt, pWorkerMode, pTarget);
            regardsAmqpAdmin.declareBinding(pTenant, queue, exchange, pWorkerMode);

            return (TenantWrapper<T>) rabbitTemplate
                    .receiveAndConvert(regardsAmqpAdmin.getQueueName(pEvt, pWorkerMode, pTarget), 0);
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }

    }
}