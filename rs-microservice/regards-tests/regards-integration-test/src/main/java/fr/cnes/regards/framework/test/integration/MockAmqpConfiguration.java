/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.integration;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;

/**
 * Class MockAmqpConfiguration Mock AMQP subscriber and publisher for intergration tests.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class MockAmqpConfiguration {

    /**
     * Subscriber mock
     *
     * @return {@link ISubscriber}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public ISubscriber eventSubscriber() {
        return Mockito.mock(ISubscriber.class);
    }

    /**
     * Subscriber mock
     *
     * @return {@link IInstanceSubscriber}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public IInstanceSubscriber eventInstanceSubscriber() {
        return Mockito.mock(IInstanceSubscriber.class);
    }

    /**
     * Publisher mock
     *
     * @return {@link IPublisher}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public IPublisher eventPublisher() {
        return Mockito.mock(IPublisher.class);
    }

    /**
     * Publisher mock
     *
     * @return {@link IPublisher}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public IInstancePublisher eventInstancePublisher() {
        return Mockito.mock(IInstancePublisher.class);
    }

}
