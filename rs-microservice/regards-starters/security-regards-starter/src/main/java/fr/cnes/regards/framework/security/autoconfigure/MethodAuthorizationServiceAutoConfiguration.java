/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.LocalTenantResolver;
import fr.cnes.regards.framework.security.endpoint.DefaultAuthorityProvider;
import fr.cnes.regards.framework.security.endpoint.DefaultPluginResourceManager;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.endpoint.IPluginResourceManager;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;

/**
 * Method Authorization Service auto configuration
 *
 * @author msordi
 *
 */
@Configuration
@ConditionalOnWebApplication
public class MethodAuthorizationServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IAuthoritiesProvider authoritiesProvider() {
        return new DefaultAuthorityProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public ITenantResolver tenantResolver() {
        return new LocalTenantResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public MethodAuthorizationService methodAuthorizationService() {
        return new MethodAuthorizationService();
    }

    @Bean
    @ConditionalOnMissingBean
    public IPluginResourceManager pluginResourceManager() {
        return new DefaultPluginResourceManager();
    }
}
