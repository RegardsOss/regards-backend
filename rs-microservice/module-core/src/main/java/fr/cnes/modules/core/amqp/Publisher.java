/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.modules.core.amqp;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author svissier
 *
 */
public class Publisher {

    @Autowired
    private RabbitAdmin rabbitAdmin_;

    @Autowired
    private RabbitTemplate rabbitTemplate_;

    public void publish(Object evt, String tenant) {
        Exchange exchange = new DirectExchange(tenant, true, false);
        rabbitAdmin_.declareExchange(exchange);
        rabbitTemplate_.convertAndSend(tenant, evt.getClass().getName(), evt);
    }

}
