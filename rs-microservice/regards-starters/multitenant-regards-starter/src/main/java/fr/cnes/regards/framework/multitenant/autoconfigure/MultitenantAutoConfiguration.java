/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.multitenant.autoconfigure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.LocalTenantResolver;
import fr.cnes.regards.framework.multitenant.test.SingleRuntimeTenantResolver;

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

    /**
     * Static tenant
     */
    @Value("${regards.tenant:#{null}}")
    private String tenant;

    @ConditionalOnMissingBean
    @Bean
    public ITenantResolver tenantResolver() {
        return new LocalTenantResolver();
    }

    /**
     *
     * This implementation is intended to be used for development purpose.<br/>
     * In production, an on request dynamic resolver must be set to retrieve request tenant.
     *
     * @return {@link IRuntimeTenantResolver}
     */
    @ConditionalOnMissingBean
    @Bean
    public IRuntimeTenantResolver threadTenantResolver() {
        return new SingleRuntimeTenantResolver(tenant);
    }
}
