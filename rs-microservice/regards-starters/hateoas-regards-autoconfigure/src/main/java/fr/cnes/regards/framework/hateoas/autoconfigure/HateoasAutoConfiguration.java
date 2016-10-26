/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.hateoas.DefaultResourceService;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.security.autoconfigure.MethodAuthorizationServiceAutoConfiguration;

/**
 *
 * HATEOAS auto configuration
 *
 * @author msordi
 *
 */
@Configuration
@AutoConfigureAfter(MethodAuthorizationServiceAutoConfiguration.class)
@ConditionalOnWebApplication
public class HateoasAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IResourceService resourceService() {
        return new DefaultResourceService();
    }
}
