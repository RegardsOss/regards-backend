/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.microservices.administration;

import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.project.service.IProjectConnectionService;
import fr.cnes.regards.modules.project.service.IProjectService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Class MicroserviceTenantResolverAutoConfigure
 * <p>
 * Autoconfiguration class for Administration Local multitenant resolver
 *
 * @author Sébastien Binda
 */
@AutoConfiguration
public class LocalTenantConnectionResolverAutoConfiguration {

    /**
     * {@link ITenantConnectionResolver} implementation for local resolver for administration service.
     *
     * @param pProjectService internal Project service.
     * @return ITenantConnectionResolver
     */
    @Bean
    @Primary
    ITenantConnectionResolver tenantConnectionResolver(IProjectService pProjectService,
                                                       IProjectConnectionService pProjectConnectionService) {
        return new LocalTenantConnectionResolver(pProjectService, pProjectConnectionService);
    }

    /**
     * {@link ITenantResolver} implementation for local tenant resolver for administration service
     *
     * @return ITenantResolver
     */
    @Bean("local-tenant-resolver")
    @Primary
    ITenantResolver tenantResolver() {
        return new LocalTenantResolver();
    }

}
