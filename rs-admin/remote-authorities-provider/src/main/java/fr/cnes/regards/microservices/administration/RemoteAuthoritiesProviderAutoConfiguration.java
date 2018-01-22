package fr.cnes.regards.microservices.administration;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.modules.accessrights.client.IMicroserviceResourceClient;
import fr.cnes.regards.modules.accessrights.client.IRoleResourceClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;

/**
 * @author Sylvain VISSIERE-GUERINET
 */

@Configuration
@EnableCaching
public class RemoteAuthoritiesProviderAutoConfiguration {

    /**
     *
     * Authorities provider by accessing administration microservice with Feign rest clients
     *
     * @param rolesClient
     *            Feign client to query administration service for roles
     * @param resourcesClient
     *            Feign client to query administration service for resources
     * @return IAuthoritiesProvider
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    IAuthoritiesProvider authoritiesProvider(final DiscoveryClient discoveryClient,
            final IMicroserviceResourceClient resourcesClient, final IRolesClient rolesClient,
            final IRuntimeTenantResolver runtimeTenantResolver, final IRoleResourceClient pRoleResourceClient) {
        return new RemoteAuthoritiesProvider(discoveryClient, resourcesClient, rolesClient, runtimeTenantResolver,
                                             pRoleResourceClient);
    }

}
