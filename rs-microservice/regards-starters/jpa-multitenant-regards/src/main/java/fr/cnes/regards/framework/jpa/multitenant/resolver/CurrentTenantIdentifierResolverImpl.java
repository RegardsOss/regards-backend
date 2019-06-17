/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.jpa.multitenant.resolver;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * Spring component used by Hibernate to determine the tenant to use during the datasource connection creation.
 * @author Sébastien Binda
 * .
 */
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentTenantIdentifierResolverImpl.class);

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public CurrentTenantIdentifierResolverImpl(IRuntimeTenantResolver pThreadTenantResolver) {
        this.runtimeTenantResolver = pThreadTenantResolver;
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = runtimeTenantResolver.getTenant() != null ? runtimeTenantResolver.getTenant() : "default";
        LOGGER.trace("Resolved tenant : {}", tenant);
        return tenant;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
