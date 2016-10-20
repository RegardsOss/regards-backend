/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microserices.administration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.modules.project.client.IProjectsClient;

/**
 *
 * Class JpaTenantConnectionConfiguration
 *
 * Test configuratiob class
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@PropertySource("classpath:dao.properties")
public class JpaTenantConnectionConfiguration {

    /**
     *
     * Stucb administration client
     *
     * @return IProjectsClient
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public IProjectsClient projectClient() {
        return new ProjectClientStub();
    }

}
