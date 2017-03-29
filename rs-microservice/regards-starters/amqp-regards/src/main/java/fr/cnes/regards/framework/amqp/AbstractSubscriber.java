/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.EventUtils;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;

/**
 *
 * Common subscriber methods
 *
 * @author Marc Sordi
 *
 */
public abstract class AbstractSubscriber implements ISubscriberContract {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSubscriber.class);

    /**
     * method from {@link fr.cnes.regards.framework.amqp.domain.IHandler}
     */
    private static final String DEFAULT_HANDLING_METHOD = "handle";

    /**
     * configuration allowing us to declare virtual host using http api and get a unique name for the instance
     */
    private final RegardsAmqpAdmin regardsAmqpAdmin;

    /**
     * Allows to retrieve {@link ConnectionFactory} per tenant
     */
    private final IRabbitVirtualHostAdmin virtualHostAdmin;

    /**
     * bean handling the conversion using {@link com.fasterxml.jackson} 2
     */
    private final Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    /**
     * Reference to running listeners by event and tenant
     */
    private final Map<Class<?>, Map<String, SimpleMessageListenerContainer>> listeners;

    public AbstractSubscriber(IRabbitVirtualHostAdmin pVirtualHostAdmin, RegardsAmqpAdmin pRegardsAmqpAdmin,
            Jackson2JsonMessageConverter pJackson2JsonMessageConverter) {
        this.virtualHostAdmin = pVirtualHostAdmin;
        this.regardsAmqpAdmin = pRegardsAmqpAdmin;
        this.jackson2JsonMessageConverter = pJackson2JsonMessageConverter;
        pJackson2JsonMessageConverter.setTypePrecedence(TypePrecedence.INFERRED);
        listeners = new HashMap<>();
    }

    @Override
    public <T extends ISubscribable> void unsubscribeFrom(Class<T> pEvent) {

        LOGGER.info("Stopping listener for event {}", pEvent.getName());

        Set<String> tenants = resolveTenants();
        Map<String, SimpleMessageListenerContainer> tenantContainers = listeners.get(pEvent);
        if (tenantContainers != null) {
            for (final String tenant : tenants) {
                SimpleMessageListenerContainer container = tenantContainers.get(tenant);
                if (container != null) {
                    container.stop();
                }
            }
        }
    }

    @Override
    public <T extends ISubscribable> void subscribeTo(Class<T> pEvent, IHandler<T> pReceiver) {
        subscribeTo(pEvent, pReceiver, WorkerMode.ALL, EventUtils.getCommunicationTarget(pEvent));
    }

    /**
     *
     * Initialize any necessary container to listen to specified event using
     * {@link AbstractSubscriber#initializeSimpleMessageListenerContainer(Class, String, IHandler, WorkerMode, Target)}
     *
     * @param <T>
     *            event type to which we subscribe
     * @param pEvt
     *            the event class token you want to subscribe to
     * @param pReceiver
     *            the POJO defining the method handling the corresponding event connection factory from context
     * @param pWorkerMode
     *            {@link WorkerMode}
     * @param pTarget
     *            communication scope
     */
    public <T> void subscribeTo(final Class<T> pEvt, final IHandler<T> pReceiver, final WorkerMode pWorkerMode,
            final Target pTarget) {

        LOGGER.info("Subscribing to event {} with target {} and mode {}", pEvt.getName(), pTarget, pWorkerMode);

        Set<String> tenants = resolveTenants();

        Map<String, SimpleMessageListenerContainer> tenantContainers = new HashMap<>();
        listeners.put(pEvt, tenantContainers);

        for (final String tenant : tenants) {
            // CHECKSTYLE:OFF
            final SimpleMessageListenerContainer container = initializeSimpleMessageListenerContainer(pEvt, tenant,
                                                                                                      pReceiver,
                                                                                                      pWorkerMode,
                                                                                                      pTarget);
            tenantContainers.put(tenant, container);

            // CHECKSTYLE:ON
            container.start();
        }
    }

    /**
     * @return the tenants on which we have to subscribe to the event
     */
    protected abstract Set<String> resolveTenants();

    /**
     *
     * @param <T>
     *            event type to which we subscribe
     * @param pEvt
     *            event we want to listen to
     * @param pTenant
     *            Tenant to listen to
     * @param pReceiver
     *            handler provided by user to handle the event reception
     * @param pWorkerMode
     *            communication Mode
     * @param pTarget
     *            communication scope
     * @return a container fully parameterized to listen to the corresponding event for the specified tenant
     */
    public <T> SimpleMessageListenerContainer initializeSimpleMessageListenerContainer(final Class<T> pEvt,
            final String pTenant, final IHandler<T> pReceiver, final WorkerMode pWorkerMode, final Target pTarget) {

        // Retrieve tenant vhost connection factory
        ConnectionFactory connectionFactory = virtualHostAdmin.getVhostConnectionFactory(pTenant);

        Queue queue;
        try {
            virtualHostAdmin.bind(pTenant);
            Exchange exchange = regardsAmqpAdmin.declareExchange(pTenant, pEvt, pWorkerMode, pTarget);
            queue = regardsAmqpAdmin.declareQueue(pTenant, pEvt, pWorkerMode, pTarget);
            regardsAmqpAdmin.declareBinding(pTenant, queue, exchange, pWorkerMode);
        } finally {
            virtualHostAdmin.unbind();
        }

        // Init container
        final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        final MessageListenerAdapter messageListener = new MessageListenerAdapter(pReceiver, DEFAULT_HANDLING_METHOD);
        messageListener.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(messageListener);
        container.addQueues(queue);
        return container;
    }
}
