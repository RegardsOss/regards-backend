/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.DataSourcesAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.modules.accessrights.client.IResourcesClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
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
@AutoConfigureBefore(DataSourcesAutoConfiguration.class)
public class MicroserviceAutoConfiguration {

    /**
     *
     * multintenantResolver
     *
     * @param pAdminProjectConnectionClient
     *            Administraction Rest client
     * @param pAdminProjectsClient
     *            Administration Rest Client
     * @return IMultitenantResolver
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    ITenantConnectionResolver multintenantResolver(ITenantConnectionClient tenantConnectionClient) {
        return new MicroserviceTenantConnectionResolver(tenantConnectionClient);
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
    IAuthoritiesProvider authoritiesProvider(final IResourcesClient pResourcesClient, final IRolesClient pRolesClient,
            IRuntimeTenantResolver runtimeTenantResolver) {
        return new MicroserviceAuthoritiesProvider(pResourcesClient, pRolesClient, runtimeTenantResolver);
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
    ITenantResolver tenantResolver(DiscoveryClient pDiscoveryClient, FeignSecurityManager pFeignSecurityManager) {
        return new RemoteTenantResolver(pDiscoveryClient, pFeignSecurityManager);
    }
}
