/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import fr.cnes.regards.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.security.utils.jwt.UserDetails;

/**
 * @author svissier
 *
 */
@Component
public class Publisher {

    @Autowired
    private RabbitAdmin rabbitAdmin_;

    @Autowired
    private RabbitTemplate rabbitTemplate_;

    /**
     *
     * @param pEvt
     *            the event you want to publish
     */
    public final void publish(Object pEvt) {
        SecurityContext context = SecurityContextHolder.getContext();
        JWTAuthentication jwt = (JWTAuthentication) context.getAuthentication();
        UserDetails userDetails = jwt.getPrincipal();
        String tenant = userDetails.getTenant();
        Exchange exchange = new DirectExchange(tenant, true, false);
        rabbitAdmin_.declareExchange(exchange);
        String binding = pEvt.getClass().getName();
        rabbitTemplate_.convertAndSend(binding, pEvt);
    }

}
