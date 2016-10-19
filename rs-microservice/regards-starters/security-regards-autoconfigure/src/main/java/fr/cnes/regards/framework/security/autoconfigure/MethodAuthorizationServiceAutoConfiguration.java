/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.security.autoconfigure.endpoint.DefaultMethodAuthorizationService;
import fr.cnes.regards.framework.security.autoconfigure.endpoint.IMethodAuthorizationService;
import fr.cnes.regards.framework.security.autoconfigure.endpoint.IPluginResourceManager;

/**
 * Method Authorization Service auto configuration
 *
 * @author msordi
 *
 */
@Configuration
@ConditionalOnWebApplication
public class MethodAuthorizationServiceAutoConfiguration {

    /**
     * Plugin resource manager. To handle plugins endpoints specific resources.
     */
    @Autowired(required = false)
    private IPluginResourceManager pluginManager;

    @Bean
    @ConditionalOnMissingBean
    public IMethodAuthorizationService methodAuthorizationService() {
        return new DefaultMethodAuthorizationService(pluginManager);
    }
}
