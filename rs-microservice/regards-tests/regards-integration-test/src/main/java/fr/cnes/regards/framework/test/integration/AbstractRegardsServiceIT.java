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

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.GsonBuilder;

import fr.cnes.regards.framework.jpa.multitenant.test.DefaultDaoTestConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.test.MockAmqpConfiguration;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.util.JUnitLogRule;

/**
 * Base class to realize integration tests using JWT and MockMvc. Should hold all the configurations to be considred by
 * any of its children.
 * Don't forget to force default tenant with
 *
 * <pre>
 * tenantResolver.forceTenant(DEFAULT_TENANT);
 * </pre>
 *
 * in
 *
 * <pre>
 * &#64;Before
 * </pre>
 *
 * annotated method for example or into tests methods.
 * <i>public</i> schema is used.
 * @author svissier
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = { DefaultTestFeignConfiguration.class, DefaultDaoTestConfiguration.class,
        MockAmqpConfiguration.class })
@ActiveProfiles({ "default", "test", "noschedule" })
@TestPropertySource(properties = { "regards.cloud.enabled=false", "spring.flyway.enabled=false" })
public abstract class AbstractRegardsServiceIT {

    private static final String DEFAULT_USER_EMAIL = "default_user@regards.fr";

    /**
     * Default user role. User {@link #getDefaultRole()} instead.
     */
    protected static final String DEFAULT_ROLE = "ROLE_DEFAULT";

    @Value("${regards.tenant:PROJECT}")
    private String defaultTenant;

    @Rule
    public JUnitLogRule rule = new JUnitLogRule();

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

    /**
     * @return class logger instance
     */
    protected Logger getLogger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Generate token for default tenant
     * @param name user name
     * @param role user role
     * @return JWT
     */
    protected String generateToken(String name, final String role) {
        return jwtService.generateToken(getDefaultTenant(), name, name, role);
    }

    /**
     * @return default role
     */
    protected String getDefaultRole() {
        return DEFAULT_ROLE;
    }

    protected String getDefaultTenant() {
        return defaultTenant;
    }

    protected String getDefaultUserEmail() {
        return DEFAULT_USER_EMAIL;
    }
}
