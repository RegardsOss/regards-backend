/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;

/**
 * Configuration class for {@link CronTest}.
 *
 * @author xbrochar
 */
@Configuration
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@PropertySource("classpath:test.properties")
public class CronTestConfiguration {

    @Bean
    public CronTest cronTest() {
        return new CronTest();
    }

    @Bean
    ISubscriber eventSubscriber() {
        return Mockito.mock(ISubscriber.class);
    }

    @Bean
    IPublisher eventPublisher() {
        return Mockito.mock(IPublisher.class);
    }
}
