/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.rest;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.amqp.IPublisher;

/**
 *
 * Class ProjectTestITConfiguration
 *
 * Configuration to mock AMQP beans.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Configuration
public class ProjectTestITConfiguration {

    /**
     *
     * Create a mock for AMQP Publisher bean
     *
     * @return {@link IPublisher}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public IPublisher publisher() {
        return Mockito.mock(IPublisher.class);
    }

}
