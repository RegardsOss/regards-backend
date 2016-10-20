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
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.configuration.AmqpConfiguration;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.provider.IProjectsProvider;

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
     *            the POJO defining the method handling the corresponding event connection factory from context
     * @param pAmqpCommunicationMode
     *            {@link AmqpCommunicationMode}
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    public final void subscribeTo(Class<?> pEvt, IHandler<?> pReceiver, AmqpCommunicationMode pAmqpCommunicationMode)
            throws RabbitMQVhostException {
        final List<String> projects = projectsProvider.retrieveProjectList();
        jackson2JsonMessageConverter.setTypePrecedence(TypePrecedence.INFERRED);
        switch (pAmqpCommunicationMode) {
            case ONE_TO_MANY:
                // ONE_TO_MANY => 1 exchange per event per project => 1 container per project => N container with one
                // queue to listen to
                for (String project : projects) {
                    // CHECKSTYLE:OFF
                    final SimpleMessageListenerContainer container = initializeSimpleMessageListenerContainer(pEvt,
                                                                                                              project,
                                                                                                              jackson2JsonMessageConverter,
                                                                                                              pReceiver);
                    // CHECKSTYLE:ON
                    startSimpleMessageListenerContainer(container);

                }
                break;
            case ONE_TO_ONE:
                // Not implemented yet, to be done if needed
                // ONE_TO_ONE => 1 exchange per project with 1 queue to listen to => 1 container per event => 1
                // container with multiple queue to listen to
                // CHECKSTYLE:OFF
                // final SimpleMessageListenerContainer container =
                // initializeSimpleMessageListenerContainer(pEvt,projects,jackson2JsonMessageConverter,pReceiver);
                // CHECKTYLE:ON
                // startSimpleMessageListenerContainer(container);
                break;
            default:
                throw new EnumConstantNotPresentException(AmqpCommunicationMode.class, pAmqpCommunicationMode.name());
        }
    }

    /**
     * @param pEvt
     *            event we want to listen to
     * @param pProjects
     *            list of all tenant to listen to
     * @param pJackson2JsonMessageConverter
     *            converter used to transcript messages
     * @param pReceiver
     *            handler provided by user to handle the event reception
     * @return 1 container to listen to each queue of all vhost for pEvt
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    public SimpleMessageListenerContainer initializeSimpleMessageListenerContainer(Class<?> pEvt,
            List<String> pProjects, Jackson2JsonMessageConverter pJackson2JsonMessageConverter, IHandler<?> pReceiver)
            throws RabbitMQVhostException {
        final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        for (String project : pProjects) {
            final CachingConnectionFactory connectionFactory = amqpConfiguration.createConnectionFactory(project);
            amqpConfiguration.addVhost(project, connectionFactory);

            final Exchange exchange = amqpConfiguration.declareExchange(pEvt.getName(),
                                                                        AmqpCommunicationMode.ONE_TO_ONE, project);
            final Queue queue = amqpConfiguration.declareQueue(pEvt, AmqpCommunicationMode.ONE_TO_ONE, project);
            amqpConfiguration.declareBinding(queue, exchange, queue.getName(), AmqpCommunicationMode.ONE_TO_ONE,
                                             project);

            container.setConnectionFactory(connectionFactory);

            final MessageListenerAdapter messageListener = new MessageListenerAdapter(pReceiver, "handle");
            messageListener.setMessageConverter(pJackson2JsonMessageConverter);
            container.setMessageListener(messageListener);
            container.addQueues(queue);
        }
        return container;
    }

    /**
     *
     * @param pEvt
     *            event we want to listen to
     * @param pProject
     *            Tenant to listen to
     * @param pJackson2JsonMessageConverter
     *            converter used to transcript messages
     * @param pReceiver
     *            handler provided by user to handle the event reception
     * @return a container fully parameterized to listen to the corresponding event for the specified tenant
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    public SimpleMessageListenerContainer initializeSimpleMessageListenerContainer(Class<?> pEvt, String pProject,
            Jackson2JsonMessageConverter pJackson2JsonMessageConverter, IHandler pReceiver)
            throws RabbitMQVhostException {
        final CachingConnectionFactory connectionFactory = amqpConfiguration.createConnectionFactory(pProject);
        amqpConfiguration.addVhost(pProject, connectionFactory);
        final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        final Exchange exchange = amqpConfiguration.declareExchange(pEvt.getName(), AmqpCommunicationMode.ONE_TO_MANY,
                                                                    pProject);
        final Queue queue = amqpConfiguration.declareQueue(pEvt, AmqpCommunicationMode.ONE_TO_MANY, pProject);
        amqpConfiguration.declareBinding(queue, exchange, queue.getName(), AmqpCommunicationMode.ONE_TO_MANY, pProject);

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
