/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import java.util.List;

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.provider.IProjectsProvider;
import fr.cnes.regards.framework.amqp.utils.IRabbitVirtualHostUtils;

/**
 * @author svissier
 *
 */
public class Subscriber {

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
    private final IRabbitVirtualHostUtils rabbitVirtualHostUtils;

    /**
     * bean handling the conversion using {@link com.fasterxml.jackson} 2
     */
    private final Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    /**
     * provider of projects allowing us to listen to any necessary RabbitMQ Vhost
     */
    @Autowired
    private IProjectsProvider projectsProvider;

    public Subscriber(RegardsAmqpAdmin pRegardsAmqpAdmin, IRabbitVirtualHostUtils pRabbitVirtualHostUtils,
            Jackson2JsonMessageConverter pJackson2JsonMessageConverter) {
        super();
        regardsAmqpAdmin = pRegardsAmqpAdmin;
        rabbitVirtualHostUtils = pRabbitVirtualHostUtils;
        jackson2JsonMessageConverter = pJackson2JsonMessageConverter;
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
     * @param pAmqpCommunicationMode
     *            {@link AmqpCommunicationMode}
     * @param pAmqpCommunicationTarget
     *            communication scope
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    public final <T> void subscribeTo(Class<T> pEvt, IHandler<T> pReceiver,
            AmqpCommunicationMode pAmqpCommunicationMode, AmqpCommunicationTarget pAmqpCommunicationTarget)
            throws RabbitMQVhostException {
        final List<String> projects = projectsProvider.retrieveProjectList();
        jackson2JsonMessageConverter.setTypePrecedence(TypePrecedence.INFERRED);

        for (String project : projects) {
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
    public <T> SimpleMessageListenerContainer initializeSimpleMessageListenerContainer(Class<T> pEvt, String pProject,
            Jackson2JsonMessageConverter pJackson2JsonMessageConverter, IHandler<T> pReceiver,
            AmqpCommunicationMode pAmqpCommunicationMode, AmqpCommunicationTarget pAmqpCommunicationTarget)
            throws RabbitMQVhostException {
        final CachingConnectionFactory connectionFactory = regardsAmqpAdmin.createConnectionFactory(pProject);
        rabbitVirtualHostUtils.addVhost(pProject, connectionFactory);
        final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        final Exchange exchange = regardsAmqpAdmin.declareExchange(pEvt, pAmqpCommunicationMode, pProject,
                                                                   pAmqpCommunicationTarget);
        final Queue queue = regardsAmqpAdmin.declareQueue(pEvt, pAmqpCommunicationMode, pProject,
                                                          pAmqpCommunicationTarget);
        regardsAmqpAdmin.declareBinding(queue, exchange, pAmqpCommunicationMode, pProject);

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
    public void startSimpleMessageListenerContainer(SimpleMessageListenerContainer pContainer) {
        pContainer.start();
    }

}
