/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.DataSourcesAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.accessrights.client.IResourcesClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;

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
     * Current Microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    @Value("${regards.microservice.admin.name}")
    private String adminMicroserviceName;

    /**
     * JWT Security service
     */
    @Autowired
    private JWTService jwtService;

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
    ITenantConnectionResolver multintenantResolver(final IProjectsClient pProjectsClient,
            final IProjectConnectionClient pProjectConnectionClient, final ITenantResolver pTenantResolver) {
        return new MicroserviceTenantConnectionResolver(microserviceName, jwtService, pProjectsClient,
                pProjectConnectionClient, pTenantResolver);
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
    IAuthoritiesProvider authoritiesProvider(final IResourcesClient pResourcesClient, final IRolesClient pRolesClient) {
        return new MicroserviceAuthoritiesProvider(microserviceName, pResourcesClient, pRolesClient);
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
    ITenantResolver tenantResolver(final FeignInitialAdminClients pInitClients) {
        return new RemoteTenantResolver(pInitClients.getProjectsClient(), jwtService, microserviceName);
    }

    /**
     *
     * Bean that contains initial Feign clients that cannot be initialized with feign annotations. Because there are
     * used too early during the spring boot process.
     *
     * @param pDiscoveryClient
     *            Eureka discovery client to find administration microservice
     * @return Feign clients for administration service
     * @since 1.0-SNAPSHOT
     */
    @Bean
    FeignInitialAdminClients initClients(final DiscoveryClient pDiscoveryClient) {
        return new FeignInitialAdminClients(pDiscoveryClient, adminMicroserviceName);
    }

}
