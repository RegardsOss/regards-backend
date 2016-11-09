/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.DataSourcesConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
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
@AutoConfigureBefore(DataSourcesConfiguration.class)
public class MicroserviceAutoConfiguration {

    /**
     * Current Microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

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
    @ConditionalOnMissingBean(ITenantConnectionResolver.class)
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    ITenantConnectionResolver multintenantResolver(final IProjectConnectionClient pAdminProjectConnectionClient,
            final IProjectsClient pAdminProjectsClient) {
        return new MicroserviceTenantConnectionResolver(pAdminProjectConnectionClient, pAdminProjectsClient,
                microserviceName, jwtService);
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
    @ConditionalOnMissingBean(IAuthoritiesProvider.class)
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    IAuthoritiesProvider authoritiesProvider(final IRolesClient pRoleClient, final IResourcesClient pResourcesClient) {
        return new MicroserviceAuthoritiesProvider(pRoleClient, pResourcesClient);
    }

}
