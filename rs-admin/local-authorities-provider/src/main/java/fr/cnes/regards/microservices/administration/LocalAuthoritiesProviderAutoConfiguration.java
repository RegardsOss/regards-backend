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
     * {@link IAuthoritiesProvider} implementation for local authorities resolver for administration service
     * <b>BEWARE:  MethodAuthorizationServiceAutoConfiguration (from security-regards-starter) provides also an
     * IAuthoritiesProvider, method name MUST NOT BE the same else Spring considers current tries to override
     * other (even with @Primary. Here, it is a problem of definition - ie name bean - NOT a priority between 2
     * implementations)</b>
     */
    @Bean
    @Primary
    public IAuthoritiesProvider localAuthoritiesProvider(IRoleService roleService, IResourcesService resourcesService,
            IRuntimeTenantResolver runtimeTenantResolver) {
        return new LocalAuthoritiesProvider(roleService, resourcesService, runtimeTenantResolver);
    }

}
