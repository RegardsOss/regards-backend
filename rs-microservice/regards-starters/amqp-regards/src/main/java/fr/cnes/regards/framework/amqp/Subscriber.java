/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import java.util.Set;

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
 *
 */
public class Subscriber implements ISubscriber {

    /**
     * method from {@link fr.cnes.regards.framework.amqp.domain.IHandler}
     */
    private static final String DEFAULT_HANDLING_METHOD = "handle";

    /**
     * configuration allowing us to declare virtual host using http api and get a unique name for the instance
     */
    private final RegardsAmqpAdmin regardsAmqpAdmin;

    /**
     * bean assisting us to manipulate virtual hosts
     */
    private final IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    /**
     * bean handling the conversion using {@link com.fasterxml.jackson} 2
     */
    private final Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    /**
     * provider of projects allowing us to listen to any necessary RabbitMQ Vhost
     */
    private final ITenantResolver tenantResolver;

    public Subscriber(final RegardsAmqpAdmin pRegardsAmqpAdmin, final IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin,
            final Jackson2JsonMessageConverter pJackson2JsonMessageConverter, final ITenantResolver pTenantResolver) {
        super();
        regardsAmqpAdmin = pRegardsAmqpAdmin;
        rabbitVirtualHostAdmin = pRabbitVirtualHostAdmin;
        jackson2JsonMessageConverter = pJackson2JsonMessageConverter;
        tenantResolver = pTenantResolver;
    }

    @Override
    public <T extends ISubscribable> void subscribeTo(Class<T> pEvent, IHandler<T> pReceiver) {
        subscribeTo(pEvent, pReceiver, WorkerMode.ALL, EventUtils.getCommunicationTarget(pEvent));
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
        final Set<String> tenants = tenantResolver.getAllTenants();
        jackson2JsonMessageConverter.setTypePrecedence(TypePrecedence.INFERRED);
        for (final String tenant : tenants) {
            // CHECKSTYLE:OFF
            final SimpleMessageListenerContainer container = initializeSimpleMessageListenerContainer(pEvt, tenant,
                                                                                                      jackson2JsonMessageConverter,
                                                                                                      pReceiver,
                                                                                                      pWorkerMode,
                                                                                                      pTarget);
            // CHECKSTYLE:ON
            startSimpleMessageListenerContainer(container);
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
        ConnectionFactory connectionFactory = rabbitVirtualHostAdmin.getVhostConnectionFactory(pTenant);

        Queue queue;
        try {
            regardsAmqpAdmin.bind(pTenant);
            Exchange exchange = regardsAmqpAdmin.declareExchange(pTenant, pEvt, pWorkerMode, pTarget);
            queue = regardsAmqpAdmin.declareQueue(pTenant, pEvt, pWorkerMode, pTarget);
            regardsAmqpAdmin.declareBinding(pTenant, queue, exchange, pWorkerMode);
        } finally {
            regardsAmqpAdmin.unbind();
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

    /**
     *
     * @param pContainer
     *            container to start
     */
    public void startSimpleMessageListenerContainer(final SimpleMessageListenerContainer pContainer) {
        pContainer.start();
    }
}
