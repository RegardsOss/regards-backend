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
 *
 * Class AccessRightsDaoTestConfiguration
 *
 * Test Configuration class
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:tests.properties")
public class AccessRightsDaoTestConfiguration {

    /**
     *
     * Mock AMQP
     *
     * @return {@link IPublisher}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public IPublisher eventPublisher() {
        return Mockito.mock(IPublisher.class);
    }

    /**
     *
     * Mock AMQP
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
     * Mock AMQP
     *
     * @return {@link IPublisher}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public IInstanceSubscriber eventInstanceSubscriber() {
        return Mockito.mock(IInstanceSubscriber.class);
    }

    /**
     *
     * Mock AMQP
     *
     * @return {@link IPublisher}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public IInstancePublisher eventInstancePublisher() {
        return Mockito.mock(IInstancePublisher.class);
    }
}
