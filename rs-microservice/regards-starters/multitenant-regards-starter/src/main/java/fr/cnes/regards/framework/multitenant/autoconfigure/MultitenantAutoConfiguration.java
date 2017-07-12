/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.multitenant.autoconfigure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.DefaultTenantResolver;
import fr.cnes.regards.framework.multitenant.test.SingleRuntimeTenantResolver;

/**
 *
 * Manage tenant resolver bean
 *
 * @author msordi
 *
 */
@Configuration
@EnableConfigurationProperties(MultitenantBootstrapProperties.class)
public class MultitenantAutoConfiguration {

    /**
     * Static tenant
     */
    @Value("${regards.tenant:#{null}}")
    private String tenant;

    @ConditionalOnMissingBean
    @Bean
    public ITenantResolver tenantResolver() {
        return new DefaultTenantResolver();
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
