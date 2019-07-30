/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.multitenant;

/**
 * In a request context, this resolver allows to retrieve request tenant. This resolver must be thread safe.
 * @author Marc Sordi
 */
public interface IRuntimeTenantResolver {

    /**
     * @return runtime tenant
     */
    String getTenant();

    /**
     * Does the current tenant is instance
     * @return true|false
     */
    boolean isInstance();

    /**
     * Force runtime tenant to a specific value on current thread.<br/>
     * We recommend to use {@link IRuntimeTenantResolver#clearTenant()} to clean the thread in a finally clause.<br/>
     * It is mostly recommended for server threads as they are reused.
     * @param tenant tenant
     */
    void forceTenant(String tenant);

    /**
     * Clear forced tenant on current thread.<br>
     * This method should only be used in the following context:<br>
     *   - Thread that can be reused by multiple tenants<br>
     *   - Thread tenant cannot only be determined thanks to authentication<br>
     * For example, server thread that handle REST call. This means it should be used only by fr.cnes.regards.framework.security.filter.JWTAuthenticationFilter.
     */
    void clearTenant();
}
