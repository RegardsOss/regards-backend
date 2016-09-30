/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import fr.cnes.regards.security.utils.jwt.UserDetails;

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

    /**
     * 0
     *
     * @param pEvt
     *            the event class token you want to subscribe to
     * @param pReceiver
     *            the POJO defining the method handling the corresponding event
     * @param pHandlingMethodName
     *            the POJO's method name defined to handle the event
     * @param pConnectionFactory
     *            connection factory from context
     * @return the container initialized with right values
     */
    public final SimpleMessageListenerContainer subscribeTo(Class<?> pEvt, Object pReceiver, String pHandlingMethodName,
            ConnectionFactory pConnectionFactory) {
        String tenant = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getTenant();
        DirectExchange exchange = new DirectExchange(tenant);
        rabbitAdmin_.declareExchange(exchange);
        Queue queue = new Queue(pEvt.getName(), true);
        rabbitAdmin_.declareQueue(queue);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(pEvt.getName());
        rabbitAdmin_.declareBinding(binding);
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(pConnectionFactory);
        container.setRabbitAdmin(rabbitAdmin_);
        MessageListenerAdapter messageListener = new MessageListenerAdapter(pReceiver, pHandlingMethodName);
        jackson2JsonMessageConverter_.setTypePrecedence(TypePrecedence.TYPE_ID);
        messageListener.setMessageConverter(jackson2JsonMessageConverter_);
        container.setMessageListener(messageListener);
        container.addQueues(queue);
        return container;
    }

}
