/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.autoconfigure;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.amqp.Poller;
import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.amqp.Subscriber;
import fr.cnes.regards.framework.amqp.configuration.AmqpConfiguration;
import fr.cnes.regards.framework.amqp.connection.RegardsSimpleRoutingConnectionFactory;

/**
 *
 * @author svissier
 *
 */
@Configuration
@ConditionalOnProperty(prefix = "regards.amqp", name = "enabled", matchIfMissing = true)
public class AmqpAutoConfiguration {

    @Bean
    public RabbitAdmin rabbitAdmin() {
        return new RabbitAdmin(regardsSimpleRoutingConnectionFactory());
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(regardsSimpleRoutingConnectionFactory());
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpConfiguration amqpConfiguration() {
        return new AmqpConfiguration();
    }

    @Bean
    public Publisher publisher() {
        return new Publisher();
    }

    @Bean
    public Subscriber subscriber() {
        return new Subscriber();
    }

    @Bean
    public Poller poller() {
        return new Poller();
    }

    @Bean
    public RegardsSimpleRoutingConnectionFactory regardsSimpleRoutingConnectionFactory() {
        return new RegardsSimpleRoutingConnectionFactory();
    }
}
