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
package fr.cnes.regards.framework.test.integration;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.jpa.multitenant.test.DefaultDaoTestConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.test.MockAmqpConfiguration;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 * Base class to realize integration tests using JWT and MockMvc. Should hold all the configurations to be considred by
 * any of its children.
 *
 * @author svissier
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { DefaultTestFeignConfiguration.class, DefaultDaoTestConfiguration.class,
        MockAmqpConfiguration.class })
@ActiveProfiles({ "default", "test" })
@TestPropertySource(locations = { "classpath:application-default.properties" })
public abstract class AbstractRegardsServiceIT {

    /**
     * Default tenant configured in application properties
     */
    protected static final String DEFAULT_TENANT = "PROJECT";

    /**
     * Default user email
     */
    protected static final String DEFAULT_USER_EMAIL = "default_user@regards.fr";

    /**
     * Default user role
     */
    protected static final String DEFAULT_ROLE = "ROLE_DEFAULT";

    /**
     * JWT service
     */
    @Autowired
    protected JWTService jwtService;

    /**
     * Global {@link GsonBuilder}
     */
    @Autowired
    protected GsonBuilder gsonBuilder;

    protected abstract Logger getLogger();

    /**
     * Generate token for default tenant
     *
     * @param pName
     *            user name
     * @param pRole
     *            user role
     * @return JWT
     */
    protected String generateToken(final String pName, final String pRole) {
        return jwtService.generateToken(DEFAULT_TENANT, pName, pRole);
    }

    protected String getDefaultRole() {
        return DEFAULT_ROLE;
    }

}
