/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Feign;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import fr.cnes.regards.client.core.TokenClientProvider;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.DataSourcesAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
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

    /**
     * JWT Security service
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Eureka discovery client
     */
    @Autowired
    private DiscoveryClient discoveryClient;

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
    ITenantConnectionResolver multintenantResolver(
            @Qualifier("initProjectsClient") final IProjectsClient pProjectsClient,
            @Qualifier("initProjectConnectionsClient") final IProjectConnectionClient pProjectConnectionClient) {
        return new MicroserviceTenantConnectionResolver(microserviceName, jwtService, pProjectsClient,
                pProjectConnectionClient);
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
    IAuthoritiesProvider authoritiesProvider(@Qualifier("initResourcesClient") final IResourcesClient pResourcesClient,
            @Qualifier("initRolesClient") final IRolesClient pRolesClient) {
        return new MicroserviceAuthoritiesProvider(microserviceName, pResourcesClient, pRolesClient);
    }

    /**
     *
     * Remote tenant resolver. Retrieve tenants from the administration service.
     *
     * @return RemoteTenantResolver
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    ITenantResolver tenantResolver(@Qualifier("initProjectsClient") final IProjectsClient pProjectsClient) {
        return new RemoteTenantResolver(pProjectsClient);
    }

    @Bean
    @Qualifier("initProjectsClient")
    IProjectsClient getProjectsClient() {
        final List<ServiceInstance> instances = discoveryClient.getInstances("rs-admin");
        if (!instances.isEmpty()) {
            return Feign.builder().contract(new SpringMvcContract()).encoder(new GsonEncoder())
                    .decoder(new ResponseEntityDecoder(new GsonDecoder()))
                    .target(new TokenClientProvider<>(IProjectsClient.class, instances.get(0).getUri().toString()));
        } else {
            return null;
        }
    }

    @Bean
    @Qualifier("initProjectConnectionsClient")
    IProjectConnectionClient getProjectConnectionsClient() {
        final List<ServiceInstance> instances = discoveryClient.getInstances("rs-admin");
        if (!instances.isEmpty()) {
            return Feign.builder().contract(new SpringMvcContract()).encoder(new GsonEncoder())
                    .decoder(new ResponseEntityDecoder(new GsonDecoder())).target(new TokenClientProvider<>(
                            IProjectConnectionClient.class, instances.get(0).getUri().toString()));
        } else {
            return null;
        }
    }

    @Bean
    @Qualifier("initResourcesClient")
    public IResourcesClient getResourcesClient() {
        final List<ServiceInstance> instances = discoveryClient.getInstances("rs-admin");
        if (!instances.isEmpty()) {
            return Feign.builder().contract(new SpringMvcContract()).encoder(new GsonEncoder())
                    .decoder(new ResponseEntityDecoder(new GsonDecoder()))
                    .target(new TokenClientProvider<>(IResourcesClient.class, instances.get(0).getUri().toString()));
        } else {
            return null;
        }
    }

    @Bean
    @Qualifier("initRolesClient")
    public IRolesClient getRolesClient() {
        final List<ServiceInstance> instances = discoveryClient.getInstances("rs-admin");
        if (!instances.isEmpty()) {
            return Feign.builder().contract(new SpringMvcContract()).encoder(new GsonEncoder())
                    .decoder(new ResponseEntityDecoder(new GsonDecoder()))
                    .target(new TokenClientProvider<>(IRolesClient.class, instances.get(0).getUri().toString()));
        } else {
            return null;
        }
    }

}
