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
package fr.cnes.regards.framework.multitenant.test;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 *
 * Single tenant resolver. Useful for testing purpose. Add multi-thread management.
 *
 * @author Marc Sordi
 * @author oroussel
 */
public class SingleRuntimeTenantResolver implements IRuntimeTenantResolver {

    // Thread safe tenant holder for forced tenant
    private static final ThreadLocal<String> tenantHolder = new ThreadLocal<>();

    public SingleRuntimeTenantResolver(final String pTenant) {
        tenantHolder.set(pTenant);
    }

    @Override
    public String getTenant() {
        return tenantHolder.get();
    }

    @Override
    public void forceTenant(final String tenant) {
        tenantHolder.set(tenant);
    }

    @Override
    public boolean isInstance() {
        return Boolean.FALSE;
    }

    @Override
    public void clearTenant() {
        tenantHolder.remove();
    }

}
