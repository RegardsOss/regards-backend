/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.test;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.ISubscriber;

/**
 * Default JPA multitenant test configuration
 *
 * @author Marc Sordi
 */
@Configuration
@EnableAutoConfiguration(exclude = JacksonAutoConfiguration.class)
@PropertySource("classpath:dao.properties")
public class DefaultTestConfiguration {

    @Bean
    public ISubscriber mockSubscriber() {
        return Mockito.mock(ISubscriber.class);
    }

    @Bean
    public IInstanceSubscriber mockInstanceSubscriber() {
        return Mockito.mock(IInstanceSubscriber.class);
    }
}
