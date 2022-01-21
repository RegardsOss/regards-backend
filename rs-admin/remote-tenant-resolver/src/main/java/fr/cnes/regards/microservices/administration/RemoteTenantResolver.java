/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.project.client.rest.ITenantClient;

/**
 *
 * Class RemoteTenantResolver
 *
 * Microservice remote tenant resolver. Retrieve tenants from the administration microservice.
 *
 * @author Sébastien Binda
 * @author Marc Sordi
 */
public class RemoteTenantResolver extends AbstractInstanceDiscoveryClientChecker implements ITenantResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteTenantEventHandler.class);

    public static final String TENANT_CACHE_NAME = "tenants";

    public static final String ACTIVE_TENANT_CACHE_NAME = "activeTenants";

    /**
     * Microservice name
     */
    private final String microserviceName;

    /**
     * Initial Feign client to administration service to retrieve informations about projects
     */
    private final ITenantClient tenantClient;

    public RemoteTenantResolver(final DiscoveryClient pDiscoveryClient, ITenantClient tenantClient,
            String microserviceName) {
        super(pDiscoveryClient);
        this.microserviceName = microserviceName;
        this.tenantClient = tenantClient;
    }

    @Cacheable(TENANT_CACHE_NAME)
    @Override
    public Set<String> getAllTenants() {
        try {
            // Bypass authorization for internal request
            FeignSecurityManager.asSystem();
            return tenantClient.getAllTenants().getBody();
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @Cacheable(ACTIVE_TENANT_CACHE_NAME)
    @Override
    public Set<String> getAllActiveTenants() {
        try {
            // Bypass authorization for internal request
            FeignSecurityManager.asSystem();
            return tenantClient.getAllActiveTenants(microserviceName).getBody();
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @CacheEvict(cacheNames = RemoteTenantResolver.TENANT_CACHE_NAME, allEntries = true)
    public void cleanTenantCache() {
        LOGGER.debug("Cleaning tenant cache");
    }

    @CacheEvict(cacheNames = RemoteTenantResolver.ACTIVE_TENANT_CACHE_NAME, allEntries = true)
    public void cleanActiveTenantCache() {
        LOGGER.debug("Cleaning active tenant cache");
    }
}
