/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.multitenant.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.LocalTenantResolver;

/**
 *
 * Manage tenant resolver bean
 *
 * @author msordi
 *
 */
@Configuration
@ConditionalOnWebApplication
public class MultitenantAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    public ITenantResolver tenantResolver() {
        return new LocalTenantResolver();
    }
}
