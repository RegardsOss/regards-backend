/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.modules.accessrights.service.resources.IResourcesService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.project.service.IProjectConnectionService;
import fr.cnes.regards.modules.project.service.IProjectService;

/**
 *
 * Class MicroserviceTenantResolverAutoConfigure
 *
 * Autoconfiguration class for Administration Local multitenant resolver
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Configuration
public class LocalTenantConnectionResolverAutoConfiguration {

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
    @Primary
    ITenantConnectionResolver tenantConnectionResolver(IProjectService pProjectService,
            IProjectConnectionService pProjectConnectionService) {
        return new LocalTenantConnectionResolver(pProjectService, pProjectConnectionService);
    }

    /**
     *
     * {@link ITenantResolver} implementation for local tenant resolver for administration service
     *
     * @return ITenantResolver
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
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
    @Primary
    IAuthoritiesProvider authoritiesProvider(IRoleService pRoleService, IResourcesService pResourcesService,
            IRuntimeTenantResolver runtimeTenantResolver) {
        return new LocalAuthoritiesProvider(pRoleService, pResourcesService, runtimeTenantResolver);
    }

}
