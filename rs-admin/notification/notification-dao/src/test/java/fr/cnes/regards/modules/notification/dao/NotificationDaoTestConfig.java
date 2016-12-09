/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.dao;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;

/**
 * Configuration class for unit testing of plugin's DAO.
 *
 * @author Christophe Mertz
 */
@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:application.properties")
public class NotificationDaoTestConfig {

    /**
     *
     * Subscriber mock
     *
     * @return {@link ISubscriber}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public ISubscriber eventSubscriber() {
        return Mockito.mock(ISubscriber.class);
    }

    /**
     *
     * Publisher mock
     *
     * @return {@link IPublisher}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public IPublisher eventPublisher() {
        return Mockito.mock(IPublisher.class);
    }
}
