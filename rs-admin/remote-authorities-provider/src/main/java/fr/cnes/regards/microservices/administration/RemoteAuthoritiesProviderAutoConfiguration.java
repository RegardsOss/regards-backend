package fr.cnes.regards.microservices.administration;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.modules.accessrights.client.IMicroserviceResourceClient;
import fr.cnes.regards.modules.accessrights.client.IRoleResourceClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.client.cache.CacheableRolesClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * @author Sylvain VISSIERE-GUERINET
 */

@Configuration
@Profile("production")
@EnableCaching
public class RemoteAuthoritiesProviderAutoConfiguration {

    /**
     * Authorities provider by accessing administration microservice with Feign rest clients.
     * Set as primary bean to override default one from security-regards-starter
     *
     * @param rolesClient     Feign client to query administration service for roles
     * @param resourcesClient Feign client to query administration service for resources
     * @return IAuthoritiesProvider
     */
    @Bean("remote-authorities-provider")
    @Primary
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    public IAuthoritiesProvider authoritiesProvider(final DiscoveryClient discoveryClient,
                                                    final IMicroserviceResourceClient resourcesClient,
                                                    final IRolesClient rolesClient,
                                                    final CacheableRolesClient cacheableRolesClient,
                                                    final IRuntimeTenantResolver runtimeTenantResolver,
                                                    final IRoleResourceClient pRoleResourceClient) {
        return new RemoteAuthoritiesProvider(discoveryClient,
                                             resourcesClient,
                                             cacheableRolesClient,
                                             rolesClient,
                                             runtimeTenantResolver,
                                             pRoleResourceClient);
    }

}
