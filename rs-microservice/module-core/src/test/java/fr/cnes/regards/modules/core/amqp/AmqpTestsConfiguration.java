/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author svissier
 *
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.security.utils", "fr.cnes.regards.modules.core" })
public class AmqpTestsConfiguration {

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory pConnectionFactory,
            Jackson2JsonMessageConverter pJackson2JsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(pConnectionFactory);
        factory.setMessageConverter(pJackson2JsonMessageConverter);
        return factory;
    }
}
