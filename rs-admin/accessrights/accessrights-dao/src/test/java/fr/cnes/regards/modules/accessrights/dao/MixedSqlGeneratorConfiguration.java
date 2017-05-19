/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;

/**
 * @author Marc Sordi
 *
 */
@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:tests-postgresql.properties")
public class MixedSqlGeneratorConfiguration {

    @Bean
    public IPublisher eventPublisher() {
        return Mockito.mock(IPublisher.class);
    }

    @Bean
    public ISubscriber eventSubscriber() {
        return Mockito.mock(ISubscriber.class);
    }

    @Bean
    public IInstanceSubscriber eventInstanceSubscriber() {
        return Mockito.mock(IInstanceSubscriber.class);
    }

    @Bean
    public IInstancePublisher eventInstancePublisher() {
        return Mockito.mock(IInstancePublisher.class);
    }
}
