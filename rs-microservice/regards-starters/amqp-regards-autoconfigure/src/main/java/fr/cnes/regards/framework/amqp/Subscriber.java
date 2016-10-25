/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import java.util.Set;

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.configuration.AmqpConfiguration;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;

/**
 * @author svissier
 *
 */
@Component
public class Subscriber {

    /**
     * method from {@link fr.cnes.regards.framework.amqp.domain.IHandler}
     */
    private static final String DEFAULT_HANDLING_METHOD = "handle";

    /**
     * configuration allowing us to declare virtual host using http api and get a unique name for the instance
     */
    @Autowired
    private AmqpConfiguration amqpConfiguration;

    /**
     * bean handling the conversion using {@link com.fasterxml.jackson} 2
     */
    @Autowired
    private Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    /**
     * provider of projects allowing us to listen to any necessary RabbitMQ Vhost
     */
    @Autowired
    private ITenantResolver tenantResolver;

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
     * @param pAmqpCommunicationMode
     *            {@link AmqpCommunicationMode}
     * @param pAmqpCommunicationTarget
     *            communication scope
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    public final <T> void subscribeTo(final Class<T> pEvt, final IHandler<T> pReceiver,
            final AmqpCommunicationMode pAmqpCommunicationMode, final AmqpCommunicationTarget pAmqpCommunicationTarget)
            throws RabbitMQVhostException {
        final Set<String> projects = tenantResolver.getAllTenants();
        jackson2JsonMessageConverter.setTypePrecedence(TypePrecedence.INFERRED);

        for (final String project : projects) {
            // CHECKSTYLE:OFF
            final SimpleMessageListenerContainer container = initializeSimpleMessageListenerContainer(pEvt, project,
                                                                                                      jackson2JsonMessageConverter,
                                                                                                      pReceiver,
                                                                                                      pAmqpCommunicationMode,
                                                                                                      pAmqpCommunicationTarget);
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
     * @param pProject
     *            Tenant to listen to
     * @param pJackson2JsonMessageConverter
     *            converter used to transcript messages
     * @param pReceiver
     *            handler provided by user to handle the event reception
     * @param pAmqpCommunicationMode
     *            communication Mode
     * @param pAmqpCommunicationTarget
     *            communication scope
     * @return a container fully parameterized to listen to the corresponding event for the specified tenant
     *
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    public <T> SimpleMessageListenerContainer initializeSimpleMessageListenerContainer(final Class<T> pEvt,
            final String pProject, final Jackson2JsonMessageConverter pJackson2JsonMessageConverter,
            final IHandler<T> pReceiver, final AmqpCommunicationMode pAmqpCommunicationMode,
            final AmqpCommunicationTarget pAmqpCommunicationTarget) throws RabbitMQVhostException {
        final CachingConnectionFactory connectionFactory = amqpConfiguration.createConnectionFactory(pProject);
        amqpConfiguration.addVhost(pProject, connectionFactory);
        final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        final Exchange exchange = amqpConfiguration.declareExchange(pEvt.getName(), pAmqpCommunicationMode, pProject,
                                                                    pAmqpCommunicationTarget);
        final Queue queue = amqpConfiguration.declareQueue(pEvt, pAmqpCommunicationMode, pProject,
                                                           pAmqpCommunicationTarget);
        amqpConfiguration.declareBinding(queue, exchange, pAmqpCommunicationMode, pProject);

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
