/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.multitenant.IThreadTenantResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.MultitenantAutoConfiguration;
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
@AutoConfigureBefore(MultitenantAutoConfiguration.class)
public class MethodAuthorizationServiceAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    public IThreadTenantResolver secureThreadTenantResolver() {
        return new SecureThreadTenantResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public IAuthoritiesProvider authoritiesProvider() {
        return new DefaultAuthorityProvider();
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
