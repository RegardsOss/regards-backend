/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microserices.administration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.microserices.administration.stubs.ProjectClientStub;
import fr.cnes.regards.microserices.administration.stubs.ProjectConnectionClientStub;
import fr.cnes.regards.modules.accessrights.client.IResourcesClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.fallback.ResourcesFallback;
import fr.cnes.regards.modules.accessrights.fallback.RolesFallback;
import fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;

/**
 *
 * Class JpaTenantConnectionConfiguration
 *
 * Test configuratiob class
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
@Configuration
@PropertySource("classpath:dao.properties")
public class JpaTenantConnectionConfiguration {

    /**
     *
     * Stub administration client
     *
     * @return IProjectsClient
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public IProjectConnectionClient projectConnectionClient() {
        return new ProjectConnectionClientStub();
    }

    /**
     *
     * Stub administration client
     *
     * @return IProjectsClient
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public IProjectsClient projectClient() {
        return new ProjectClientStub();
    }

    /**
     *
     * Stub administration client
     *
     * @return IProjectsClient
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public IRolesClient roleClient() {
        return new RolesFallback();
    }

    /**
     *
     * Stub administration client
     *
     * @return IProjectsClient
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public IResourcesClient resourceClient() {
        return new ResourcesFallback();
    }

}
