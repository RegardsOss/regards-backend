/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.modules.core.amqp;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.security.utils.jwt.UserDetails;

/**
 * @author svissier
 *
 */
public class Publisher {

    @Autowired
    private RabbitAdmin rabbitAdmin_;

    @Autowired
    private RabbitTemplate rabbitTemplate_;

    /**
     *
     * @param evt
     *            the event you want to publish
     */
    public void publish(Object evt) {
        String tenant = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getTenant();
        Exchange exchange = new DirectExchange(tenant, true, false);
        rabbitAdmin_.declareExchange(exchange);
        rabbitTemplate_.convertAndSend(tenant, evt.getClass().getName(), evt);
    }

}
