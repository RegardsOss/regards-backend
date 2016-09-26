/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.modules.core.amqp;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.security.utils.jwt.UserDetails;

/**
 * @author svissier
 *
 */
public class Subscriber {

    @Autowired
    private RabbitAdmin rabbitAdmin_;

    /**
     *
     * @param evt
     *            the event you want to subscribe to
     * @param receiver
     *            the POJO defining the method handling the corresponding evt
     * @param handlingMethodName
     *            the POJO's method name defined to handle the evt
     * @param connectionFactory
     * @return
     */
    public MessageListenerContainer subscribeTo(Object evt, Object receiver, String handlingMethodName,
            ConnectionFactory connectionFactory) {
        String tenant = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getTenant();
        DirectExchange exchange = new DirectExchange(tenant);
        rabbitAdmin_.declareExchange(exchange);
        Queue queue = new Queue(evt.getClass().getName(), true);
        rabbitAdmin_.declareQueue(queue);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(evt.getClass().getName());
        rabbitAdmin_.declareBinding(binding);
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setRabbitAdmin(rabbitAdmin_);
        MessageListener messageListener = new MessageListenerAdapter(receiver, handlingMethodName);
        container.setMessageListener(messageListener);
        return container;
    }

}
