/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

import java.util.List;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.core.amqp.configuration.AmqpConfiguration;
import fr.cnes.regards.modules.core.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.modules.core.amqp.domain.IHandler;
import fr.cnes.regards.modules.core.amqp.provider.IProjectsProvider;
import fr.cnes.regards.modules.core.exception.AddingRabbitMQVhostException;
import fr.cnes.regards.modules.core.exception.AddingRabbitMQVhostPermissionException;

/**
 * @author svissier
 *
 */
@Component
public class Subscriber {

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
    private IProjectsProvider projectsProvider;

    /**
     *
     * initialize any necessary container to listen to all tenant provided by the provider for the specified element
     *
     * @param pEvt
     *            the event class token you want to subscribe to
     * @param pReceiver
     *            the POJO defining the method handling the corresponding event
     * @param pConnectionFactory
     *            connection factory from context
     * @throws AddingRabbitMQVhostException
     *             represent any error that could occur while trying to add the new Vhost
     * @throws AddingRabbitMQVhostPermissionException
     *             represent any error that could occur while adding the permission to the specified vhost
     */
    public final void subscribeTo(Class<?> pEvt, IHandler<?> pReceiver, ConnectionFactory pConnectionFactory)
            throws AddingRabbitMQVhostException, AddingRabbitMQVhostPermissionException {
        final List<String> projects = projectsProvider.retrieveProjectList();
        jackson2JsonMessageConverter.setTypePrecedence(TypePrecedence.INFERRED);
        for (String project : projects) {
            // CHECKSTYLE:OFF
            final SimpleMessageListenerContainer container = initializeSimpleMessageListenerContainer(pEvt,
                                                                                                      pConnectionFactory,
                                                                                                      project,
                                                                                                      jackson2JsonMessageConverter,
                                                                                                      pReceiver);
            // CHECKSTYLE:ON
            startSimpleMessageListenerContainer(container);
        }
    }

    /**
     *
     * @param pEvt
     *            event we want to listen to
     * @param pConnectionFactory
     *            bean initialized by spring to get host and port of the RabbitMQ server
     * @param pProject
     *            Tenant to listen to
     * @param pJackson2JsonMessageConverter
     *            converter used to transcript messages
     * @param pReceiver
     *            handler provided by user to handle the event reception
     * @return a container fully parameterized to listen to the corresponding event for the specified tenant
     * @throws AddingRabbitMQVhostException
     *             represent any error that could occur while trying to add the new Vhost
     * @throws AddingRabbitMQVhostPermissionException
     *             represent any error that could occur while adding the permission to the specified vhost
     */
    public SimpleMessageListenerContainer initializeSimpleMessageListenerContainer(Class<?> pEvt,
            ConnectionFactory pConnectionFactory, String pProject,
            Jackson2JsonMessageConverter pJackson2JsonMessageConverter, IHandler pReceiver)
            throws AddingRabbitMQVhostException, AddingRabbitMQVhostPermissionException {
        amqpConfiguration.addVhost(pProject);
        final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        // for now subscription is only for ONE_TO_MANY purpose
        final Exchange exchange = amqpConfiguration.declareExchange(pEvt.getName(), AmqpCommunicationMode.ONE_TO_MANY,
                                                                    pProject);
        final Queue queue = amqpConfiguration.declarequeue(pEvt.getClass(), AmqpCommunicationMode.ONE_TO_MANY,
                                                           pProject);
        final Binding binding = amqpConfiguration.declareBinding(queue, exchange, "", AmqpCommunicationMode.ONE_TO_MANY,
                                                                 pProject);

        final CachingConnectionFactory connectionFactory = amqpConfiguration.getRabbitConnectionFactory();
        connectionFactory.setVirtualHost(pProject);
        container.setConnectionFactory(connectionFactory);

        final MessageListenerAdapter messageListener = new MessageListenerAdapter(pReceiver, "handle");
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
