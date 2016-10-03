/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author svissier
 *
 */
@Component
public class Subscriber {

    @Autowired
    private RabbitAdmin rabbitAdmin_;

    @Autowired
    private Jackson2JsonMessageConverter jackson2JsonMessageConverter_;

    @Autowired
    private IProjectsProvider projectsProvider_;

    /**
     *
     *
     * @param pEvt
     *            the event class token you want to subscribe to
     * @param pReceiver
     *            the POJO defining the method handling the corresponding event
     * @param pConnectionFactory
     *            connection factory from context
     * @return the container initialized with right values
     */
    public final SimpleMessageListenerContainer subscribeTo(Class<?> pEvt, Handler pReceiver,
            ConnectionFactory pConnectionFactory) {
        List<String> projects = projectsProvider_.retrieveProjectList();
        List<DirectExchange> exchanges = projects.stream().map(p -> new DirectExchange(p)).collect(Collectors.toList());
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        jackson2JsonMessageConverter_.setTypePrecedence(TypePrecedence.TYPE_ID);
        for (DirectExchange exchange : exchanges) {
            rabbitAdmin_.declareExchange(exchange);
            Queue queue = new Queue(pEvt.getName(), true);
            rabbitAdmin_.declareQueue(queue);
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(pEvt.getName());
            rabbitAdmin_.declareBinding(binding);
            container.setConnectionFactory(pConnectionFactory);
            container.setRabbitAdmin(rabbitAdmin_);
            MessageListenerAdapter messageListener = new MessageListenerAdapter(new TenantWrapperReceiver(pReceiver),
                    "dewrap");
            messageListener.setMessageConverter(jackson2JsonMessageConverter_);
            container.setMessageListener(messageListener);
            container.addQueues(queue);
        }
        return container;
    }

}
