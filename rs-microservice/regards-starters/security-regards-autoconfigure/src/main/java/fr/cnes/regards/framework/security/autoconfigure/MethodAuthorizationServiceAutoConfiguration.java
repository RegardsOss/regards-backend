/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.security.autoconfigure.endpoint.DefaultMethodAuthorizationService;
import fr.cnes.regards.framework.security.autoconfigure.endpoint.IMethodAuthorizationService;

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
    public IMethodAuthorizationService methodAuthorizationService() {
        return new DefaultMethodAuthorizationService();
    }
}
