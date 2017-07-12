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
package fr.cnes.regards.framework.jpa.multitenant.test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * Default configuration test
 *
 * @author Marc Sordi
 *
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { DefaultTestConfiguration.class })
public abstract class AbstractDaoTest {

    /**
     * Default tenant configured in dao.properties
     */
    private static final String DEFAULT_TENANT = "PROJECT";

    /**
     * JPA entity manager : use it to flush context to prevent false positive
     * @see MultitenantJpaAutoConfiguration#PERSITENCE_UNIT_NAME for unit name
     */
    @PersistenceContext(unitName = "multitenant")
    protected EntityManager entityManager;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    protected void injectDefaultToken() {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
    }

    protected void injectToken(String pTenant) {
        runtimeTenantResolver.forceTenant(pTenant);
    }
}
