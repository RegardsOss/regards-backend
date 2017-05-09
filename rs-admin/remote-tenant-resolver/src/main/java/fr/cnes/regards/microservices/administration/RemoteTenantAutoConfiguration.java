/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.DataSourcesAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.MultitenantJpaAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.modules.accessrights.client.IMicroserviceResourceClient;
import fr.cnes.regards.modules.accessrights.client.IRoleResourceClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.project.client.rest.ITenantClient;
import fr.cnes.regards.modules.project.client.rest.ITenantConnectionClient;

/**
 *
 * Class MicroserviceTenantResolverAutoConfigure
 *
 * Autoconfiguration class for Microservices multitenant resolver
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Configuration
@AutoConfigureBefore({ DataSourcesAutoConfiguration.class, MultitenantJpaAutoConfiguration.class })
public class RemoteTenantAutoConfiguration {

    /**
     * Microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     *
     * @param tenantConnectionClient
     *            Feign clien
     * @return {@link ITenantConnectionResolver}
     */
    @Bean
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    ITenantConnectionResolver multitenantResolver(final DiscoveryClient discoveryClient,
            final ITenantConnectionClient tenantConnectionClient) {
        return new RemoteTenantConnectionResolver(discoveryClient, tenantConnectionClient);
    }

    /**
     *
     * Authorities provider by accessing administration microservice with Feign rest clients
     *
     * @param pRoleClient
     *            Feign client to query administration service for roles
     * @param pResourcesClient
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

    /**
     *
     * Remote tenant resolver. Retrieve tenants from the administration service.
     *
     * @param pInitClients
     *            Do not use the IProjectsClient. It must not be initialized at this time. To do so, we use the specials
     *            administration clients manually configured {@link FeignInitialAdminClients}
     *
     * @return RemoteTenantResolver
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    ITenantResolver tenantResolver(final DiscoveryClient pDiscoveryClient, final ITenantClient tenantClient) {
        return new RemoteTenantResolver(pDiscoveryClient, tenantClient, microserviceName);
    }
}
