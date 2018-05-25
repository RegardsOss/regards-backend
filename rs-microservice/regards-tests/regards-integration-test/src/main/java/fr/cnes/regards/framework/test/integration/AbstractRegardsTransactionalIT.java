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
package fr.cnes.regards.framework.test.integration;

import org.springframework.test.context.transaction.BeforeTransaction;

/**
 * Add default transactional initialization
 *
 * @author Marc Sordi
 *
 */
public abstract class AbstractRegardsTransactionalIT extends AbstractRegardsIT {

    @BeforeTransaction
    protected void beforeTransaction() {
        injectToken(DEFAULT_TENANT, DEFAULT_ROLE);
    }

    /**
     * Inject token in the security context.<br>
     * Override this method to manage your tenant and role in transaction
     *
     * @param pTenant
     *            tenant
     * @param pRole
     *            role
     */
    protected void injectToken(String pTenant, String pRole) {
        jwtService.injectMockToken(pTenant, pRole);
    }

}
