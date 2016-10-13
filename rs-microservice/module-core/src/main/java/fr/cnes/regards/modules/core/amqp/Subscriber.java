/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

import java.util.List;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.core.amqp.configuration.AmqpConfiguration;
import fr.cnes.regards.modules.core.amqp.provider.IProjectsProvider;
import fr.cnes.regards.modules.core.amqp.utils.IHandler;
import fr.cnes.regards.modules.core.exception.AddingRabbitMQVhostException;
import fr.cnes.regards.modules.core.exception.AddingRabbitMQVhostPermissionException;

/**
 * @author svissier
 *
 */
@Component
public class Subscriber {

    @Autowired
    private RabbitAdmin rabbitAdmin;

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
     * @throws AddingRabbitMQVhostPermissionException
     * @throws AddingRabbitMQVhostException
     */
    public final void subscribeTo(Class<?> pEvt, IHandler<?> pReceiver, ConnectionFactory pConnectionFactory)
            throws AddingRabbitMQVhostException, AddingRabbitMQVhostPermissionException {
        final List<String> projects = projectsProvider.retrieveProjectList();
        jackson2JsonMessageConverter.setTypePrecedence(TypePrecedence.INFERRED);

        for (String project : projects) {
            // CHECKSTYLE:OFF
            final SimpleMessageListenerContainer container = initializeSimpleMessageListenerContainer(rabbitAdmin, pEvt,
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
     * @param pRabbitAdmin
     *            bean initialize by spring-boot to manage RabbitMQ
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
     * @throws AddingRabbitMQVhostPermissionException
     * @throws AddingRabbitMQVhostException
     */
    public SimpleMessageListenerContainer initializeSimpleMessageListenerContainer(RabbitAdmin pRabbitAdmin,
            Class<?> pEvt, ConnectionFactory pConnectionFactory, String pProject,
            Jackson2JsonMessageConverter pJackson2JsonMessageConverter, IHandler pReceiver)
            throws AddingRabbitMQVhostException, AddingRabbitMQVhostPermissionException {
        amqpConfiguration.addVhost(pProject);
        ((CachingConnectionFactory) rabbitAdmin.getRabbitTemplate().getConnectionFactory()).setVirtualHost(pProject);
        final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        final DirectExchange exchange = new DirectExchange("REGARDS");
        pRabbitAdmin.declareExchange(exchange);
        final Queue queue = new Queue(pEvt.getName(), true);
        pRabbitAdmin.declareQueue(queue);
        final Binding binding = BindingBuilder.bind(queue).to(exchange).with(pEvt.getName());
        pRabbitAdmin.declareBinding(binding);

        // container.setConnectionFactory(amqpConfiguration.getConnectionFactory(pProject));
        container.setConnectionFactory(pRabbitAdmin.getRabbitTemplate().getConnectionFactory());
        // container.setRabbitAdmin(pRabbitAdmin);
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
