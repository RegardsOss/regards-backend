/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.security.autoconfigure.test;

import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.security.autoconfigure.SecureRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 * Overrides {@link SecureRuntimeTenantResolver} for test context to avoid clear teant.
 * @author Sébastien Binda
 */
public class SecureTestRuntimeTenantResolver extends SecureRuntimeTenantResolver {

    /**
     * @param pInstanceTenantName
     */
    public SecureTestRuntimeTenantResolver(String pInstanceTenantName) {
        super(pInstanceTenantName);
    }

    @Override
    public void clearTenant() {
        LOGGER.info("WARNING : Tenant clear is disabled in test context !!!!!");
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        // when we clear the tenant, system will act by getting it from the security context holder, so we do the same
        // for logging
        if (authentication != null) {
            MDC.put(TENANT, authentication.getTenant());
        }
    }
}
