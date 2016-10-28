/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.ITenantConnectionResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.modules.project.service.IProjectConnectionService;
import fr.cnes.regards.modules.project.service.IProjectService;

/**
 *
 * Class MicroserviceTenantResolverAutoConfigure
 *
 * Autoconfiguration class for Administration Local multitenant resolver
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
public class LocalTenantConnectionResolverAutoConfigure {

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     *
     * {@link ITenantConnectionResolver} implementation for local resolver for administration service.
     *
     * @param pProjectService
     *            internal Project service.
     * @return ITenantConnectionResolver
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @ConditionalOnMissingBean
    ITenantConnectionResolver tenantConnectionResolver(final IProjectService pProjectService,
            final IProjectConnectionService pProjectConnectionService) {
        return new LocalTenantConnectionResolver(microserviceName, pProjectService, pProjectConnectionService);
    }

    /**
     *
     * {@link ITenantResolver} implementation for local tenant resolver for administration service
     *
     * @return ITenantResolver
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @ConditionalOnMissingBean
    ITenantResolver tenantResolver() {
        return new LocalTenantResolver();
    }

    /**
     *
     * {@link IAuthoritiesProvider} implementation for local authorities resolver for administration service
     *
     * @return IAuthoritiesProvider
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @ConditionalOnMissingBean
    IAuthoritiesProvider authoritiesProvider() {
        return new LocalAuthoritiesProvider();
    }

}
