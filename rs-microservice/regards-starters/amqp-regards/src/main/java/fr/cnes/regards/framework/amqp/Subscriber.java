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
import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 * @author svissier
 * @author Marc Sordi
 *
 */
public class Subscriber implements ISubscriber {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Subscriber.class);

    /**
     * method from {@link fr.cnes.regards.framework.amqp.domain.IHandler}
     */
    private static final String DEFAULT_HANDLING_METHOD = "handle";

    /**
     * configuration allowing us to declare virtual host using http api and get a unique name for the instance
     */
    private final RegardsAmqpAdmin regardsAmqpAdmin;

    /**
     * bean handling the conversion using {@link com.fasterxml.jackson} 2
     */
    private final Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    /**
     * provider of projects allowing us to listen to any necessary RabbitMQ Vhost
     */
    private final ITenantResolver tenantResolver;

    /**
     * Allows to retrieve {@link ConnectionFactory} per tenant
     */
    private final IRabbitVirtualHostAdmin virtualHostAdmin;

    /**
     * Reference to running listeners by event and tenant
     */
    private final Map<Class<?>, Map<String, SimpleMessageListenerContainer>> listeners;

    public Subscriber(IRabbitVirtualHostAdmin pVirtualHostAdmin, RegardsAmqpAdmin pRegardsAmqpAdmin,
            Jackson2JsonMessageConverter pJackson2JsonMessageConverter, ITenantResolver pTenantResolver) {
        super();
        listeners = new HashMap<>();
        this.virtualHostAdmin = pVirtualHostAdmin;
        regardsAmqpAdmin = pRegardsAmqpAdmin;
        jackson2JsonMessageConverter = pJackson2JsonMessageConverter;
        tenantResolver = pTenantResolver;
    }

    @Override
    public <T extends ISubscribable> void subscribeTo(Class<T> pEvent, IHandler<T> pReceiver) {
        subscribeTo(pEvent, pReceiver, WorkerMode.ALL, EventUtils.getCommunicationTarget(pEvent));
    }

    @Override
    public <T extends ISubscribable> void unsubscribeFrom(Class<T> pEvent) {

        LOGGER.info("Stopping listener for event {}", pEvent.getName());

        Set<String> tenants = tenantResolver.getAllTenants();
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

    /**
     *
     * initialize any necessary container to listen to all tenant provided by the provider for the specified element
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
    public final <T> void subscribeTo(final Class<T> pEvt, final IHandler<T> pReceiver, final WorkerMode pWorkerMode,
            final Target pTarget) {

        LOGGER.info("Subscribing to event {} with target {} and mode {}", pEvt.getName(), pTarget, pWorkerMode);

        Set<String> tenants = tenantResolver.getAllTenants();
        jackson2JsonMessageConverter.setTypePrecedence(TypePrecedence.INFERRED);

        Map<String, SimpleMessageListenerContainer> tenantContainers = new HashMap<>();
        listeners.put(pEvt, tenantContainers);

        for (final String tenant : tenants) {
            // CHECKSTYLE:OFF
            final SimpleMessageListenerContainer container = initializeSimpleMessageListenerContainer(pEvt, tenant,
                                                                                                      jackson2JsonMessageConverter,
                                                                                                      pReceiver,
                                                                                                      pWorkerMode,
                                                                                                      pTarget);
            tenantContainers.put(tenant, container);

            // CHECKSTYLE:ON
            container.start();
        }
    }

    /**
     *
     * @param <T>
     *            event type to which we subscribe
     * @param pEvt
     *            event we want to listen to
     * @param pTenant
     *            Tenant to listen to
     * @param pJackson2JsonMessageConverter
     *            converter used to transcript messages
     * @param pReceiver
     *            handler provided by user to handle the event reception
     * @param pWorkerMode
     *            communication Mode
     * @param pTarget
     *            communication scope
     * @return a container fully parameterized to listen to the corresponding event for the specified tenant
     */
    public <T> SimpleMessageListenerContainer initializeSimpleMessageListenerContainer(final Class<T> pEvt,
            final String pTenant, final Jackson2JsonMessageConverter pJackson2JsonMessageConverter,
            final IHandler<T> pReceiver, final WorkerMode pWorkerMode, final Target pTarget) {

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
        messageListener.setMessageConverter(pJackson2JsonMessageConverter);
        container.setMessageListener(messageListener);
        container.addQueues(queue);
        return container;
    }
}
