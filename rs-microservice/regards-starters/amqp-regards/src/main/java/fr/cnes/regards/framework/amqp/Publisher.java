/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.MultitenantAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.RabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.EventUtils;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * @author svissier
 * @author lmieulet
 * @author Marc Sordi
 *
 */
public class Publisher implements IPublisher {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Publisher.class);

    /**
     * Default routing key
     */
    private static final String DEFAULT_ROUTING_KEY = "";

    /**
     * bean allowing us to send message to the broker
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * configuration initializing required bean
     */
    private final MultitenantAmqpAdmin regardsAmqpAdmin;

    /**
     * bean assisting us to manipulate virtual host
     */
    private final IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    /**
     * Resolve thread tenant
     */
    private final IRuntimeTenantResolver threadTenantResolver;

    public Publisher(final RabbitTemplate pRabbitTemplate, final MultitenantAmqpAdmin pRegardsAmqpAdmin,
            final IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin, IRuntimeTenantResolver pThreadTenantResolver) {
        super();
        rabbitTemplate = pRabbitTemplate;
        regardsAmqpAdmin = pRegardsAmqpAdmin;
        rabbitVirtualHostAdmin = pRabbitVirtualHostAdmin;
        this.threadTenantResolver = pThreadTenantResolver;
    }

    @Override
    public <T extends ISubscribable> void publish(T pEvent) {
        publish(pEvent, 0);
    }

    @Override
    public <T extends ISubscribable> void publish(T pEvent, int pPriority) {
        Class<?> eventClass = pEvent.getClass();
        publish(pEvent, WorkerMode.ALL, EventUtils.getCommunicationTarget(eventClass), pPriority);
    }

    @Override
    public <T extends IPollable> void publish(T pEvent) {
        publish(pEvent, 0);
    }

    @Override
    public <T extends IPollable> void publish(T pEvent, int pPriority) {
        Class<?> eventClass = pEvent.getClass();
        publish(pEvent, EventUtils.getCommunicationMode(eventClass), EventUtils.getCommunicationTarget(eventClass),
                pPriority);
    }

    /**
     * @param <T>
     *            event to be published
     * @param pEvt
     *            the event you want to publish
     * @param pPriority
     *            priority given to the event
     * @param pWorkerMode
     *            publishing mode
     * @param pTarget
     *            publishing scope
     */
    public final <T> void publish(final T pEvt, final WorkerMode pWorkerMode, final Target pTarget,
            final int pPriority) {

        String tenant = threadTenantResolver.getTenant();
        if (tenant != null) {
            publish(tenant, pEvt, pWorkerMode, pTarget, pPriority);
        } else {
            LOGGER.error("[AMQP Publisher] Unable to publish event {} because no tenant found.", pEvt.getClass());
        }
    }

    /**
     * @param <T>
     *            event to be published
     * @param pTenant
     *            the tenant name
     * @param pEvt
     *            the event you want to publish
     * @param pPriority
     *            priority given to the event
     * @param pWorkerMode
     *            publishing mode
     * @param pTarget
     *            publishing scope
     */
    private final <T> void publish(final String pTenant, final T pEvt, final WorkerMode pWorkerMode,
            final Target pTarget, final int pPriority) {

        final Class<?> evtClass = pEvt.getClass();

        // add the Vhost corresponding to this tenant
        rabbitVirtualHostAdmin.addVhost(pTenant);

        // Declare exchange
        final Exchange exchange = regardsAmqpAdmin.declareExchange(pTenant, evtClass, pWorkerMode, pTarget);

        if (WorkerMode.SINGLE.equals(pWorkerMode)) {
            // Direct exchange needs a specific queue, a binding between this queue and exchange containing a specific
            // routing key
            final Queue queue = regardsAmqpAdmin.declareQueue(pTenant, pEvt.getClass(), WorkerMode.SINGLE, pTarget);
            regardsAmqpAdmin.declareBinding(pTenant, queue, exchange, pWorkerMode);
            publishMessageByTenant(pTenant, exchange.getName(),
                                   regardsAmqpAdmin.getRoutingKey(queue.getName(), pWorkerMode), pEvt, pPriority);
        } else
            if (WorkerMode.ALL.equals(pWorkerMode)) {
                // Routing key useless ... always skipped with a fanout exchange
                publishMessageByTenant(pTenant, exchange.getName(), DEFAULT_ROUTING_KEY, pEvt, pPriority);
            } else {
                String errorMessage = String.format("Unexpected communication mode : %s.", pWorkerMode);
                LOGGER.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
    }

    /**
     * Publish event in tenant virtual host
     *
     * @param <T>
     *            event type
     * @param pTenant
     *            tenant
     * @param pExchangeName
     *            {@link Exchange} name
     * @param pRoutingKey
     *            routing key (really useful for direct exchange). Use {@link Publisher#DEFAULT_ROUTING_KEY} for fanout.
     * @param pEvt
     *            the event to publish
     * @param pPriority
     *            the event priority
     */
    private final <T> void publishMessageByTenant(final String pTenant, String pExchangeName, String pRoutingKey,
            final T pEvt, final int pPriority) {

        // Message to publish
        final TenantWrapper<T> messageSended = new TenantWrapper<>(pEvt, pTenant);

        // Bind the connection to the right vHost (i.e. tenant to publish the message)
        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), RabbitVirtualHostAdmin.getVhostName(pTenant));
        // routing key is unnecessary for fanout exchanges but is for direct exchanges
        rabbitTemplate.convertAndSend(pExchangeName, pRoutingKey, messageSended, pMessage -> {
            final MessageProperties propertiesWithPriority = pMessage.getMessageProperties();
            propertiesWithPriority.setPriority(pPriority);
            return new Message(pMessage.getBody(), propertiesWithPriority);
        });
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
    }
}
