/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.integration;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;

/**
 *
 * Class MockAmqpConfiguration
 *
 * Mock AMQP subscriber and publisher for intergration tests.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class MockAmqpConfiguration {

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
