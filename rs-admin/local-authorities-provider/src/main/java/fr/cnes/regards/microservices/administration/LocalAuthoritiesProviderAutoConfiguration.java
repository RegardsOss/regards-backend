package fr.cnes.regards.microservices.administration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.modules.accessrights.service.resources.IResourcesService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
public class LocalAuthoritiesProviderAutoConfiguration {

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
