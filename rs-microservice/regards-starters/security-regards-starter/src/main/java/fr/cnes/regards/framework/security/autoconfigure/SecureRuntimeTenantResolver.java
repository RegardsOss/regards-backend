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
package fr.cnes.regards.framework.security.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 * Retrieve thread tenant according to security context
 *
 * @author Marc Sordi
 *
 */
public class SecureRuntimeTenantResolver implements IRuntimeTenantResolver {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SecureRuntimeTenantResolver.class);

    // Thread safe tenant holder for forced tenant
    private static final ThreadLocal<String> tenantHolder = new ThreadLocal<>();

    /**
     * Name of the static and fixed name of instance virtual tenant.
     */
    private final String instanceTenantName;

    public SecureRuntimeTenantResolver(String pInstanceTenantName) {
        super();
        this.instanceTenantName = pInstanceTenantName;
    }

    @Override
    public String getTenant() {
        // Try to get tenant from tenant holder
        final String tenant = tenantHolder.get();
        if (tenant != null) {
            return tenant;
        }
        // Try to get tenant from JWT
        final JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication != null) {
            return authentication.getTenant();
        } else {
            return null;
        }
    }

    @Override
    public void forceTenant(final String pTenant) {
        // when we force the tenant for the application, we set it for logging too
        MDC.put("tenant", pTenant);
        tenantHolder.set(pTenant);
    }

    @Override
    public void clearTenant() {
        LOGGER.debug("Clearing tenant");
        tenantHolder.remove();
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        // when we clear the tenant, system will act by getting it from the security context holder, so we do the same for logging
        if(authentication!=null) {
            MDC.put("tenant", authentication.getTenant());
        } else {
            MDC.put("tenant", null);
        }
    }

    @Override
    public Boolean isInstance() {
        return instanceTenantName.equals(getTenant());
    }
}
