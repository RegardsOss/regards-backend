/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import fr.cnes.regards.modules.core.amqp.configuration.AmqpConfiguration;
import fr.cnes.regards.modules.core.amqp.utils.TenantWrapper;
import fr.cnes.regards.modules.core.exception.AddingRabbitMQVhostException;
import fr.cnes.regards.modules.core.exception.AddingRabbitMQVhostPermissionException;
import fr.cnes.regards.security.utils.jwt.JWTAuthentication;

/**
 * @author svissier
 *
 */
@Component
public class Publisher {

    @Autowired
    private RabbitAdmin rabbitAdmin;

    /**
     * bean allowing us to send message to the broker
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * configuration initializing required bean
     */
    @Autowired
    private AmqpConfiguration amqpConfiguration;

    /**
     *
     * @param pEvt
     *            the event you want to publish
     * @throws AddingRabbitMQVhostException
     *             represent any error that could occur while trying to add the new Vhost
     * @throws AddingRabbitMQVhostPermissionException
     *             represent any error that could occur while adding the permission to the specified vhost
     * @throws JsonProcessingException
     */
    public final void publish(Object pEvt) throws AddingRabbitMQVhostException, AddingRabbitMQVhostPermissionException {
        final String tenant = ((JWTAuthentication) SecurityContextHolder.getContext().getAuthentication())
                .getPrincipal().getTenant();
        // add the Vhost corresponding to this tenant
        amqpConfiguration.addVhost(tenant);
        final Exchange exchange = new DirectExchange("REGARDS", true, false);
        ((CachingConnectionFactory) rabbitAdmin.getRabbitTemplate().getConnectionFactory()).setVirtualHost(tenant);
        rabbitAdmin.declareExchange(exchange);
        final String binding = pEvt.getClass().getName();
        final TenantWrapper messageSended = new TenantWrapper<>(pEvt, tenant);
        // bind the connection to the right vHost ie tenant to publish the message
        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), tenant);
        rabbitTemplate.convertAndSend(binding, messageSended);
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
    }

}
