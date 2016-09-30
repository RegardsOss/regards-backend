/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author svissier
 *
 */
@Configuration
public class AmqpConfiguration {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory pConnectionFactory) {
        return new RabbitAdmin(pConnectionFactory);
    }
}
