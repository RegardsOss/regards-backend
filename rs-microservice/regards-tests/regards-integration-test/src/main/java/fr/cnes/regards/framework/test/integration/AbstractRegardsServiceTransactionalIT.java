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
package fr.cnes.regards.framework.test.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * >
 * Add default transactional initialization
 * @author Christophe Mertz
 */
public abstract class AbstractRegardsServiceTransactionalIT extends AbstractRegardsServiceIT {

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    /**
     * Set the tenant before a transaction
     */
    @BeforeTransaction
    protected void beforeTransaction() {
        tenantResolver.forceTenant(getDefaultTenant());
    }

}
