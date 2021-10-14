/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.jpa.multitenant.test.AppDaoTestConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.test.MockAmqpConfiguration;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.util.JUnitLogRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Base class to realize integration tests using JWT and MockMvc. Should hold all the configurations to be considred by
 * any of its children.
 * Don't forget to force default tenant with
 *
 * <pre>
 * tenantResolver.forceTenant(DEFAULT_TENANT);
 * </pre>
 * <p>
 * in
 *
 * <pre>
 * &#64;Before
 * </pre>
 * <p>
 * annotated method for example or into tests methods.
 * <i>public</i> schema is used.
 *
 * @author svissier
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(
        classes = { DefaultTestFeignConfiguration.class, AppDaoTestConfiguration.class, MockAmqpConfiguration.class })
@ActiveProfiles({ "default", "test", "noscheduler" })
@TestPropertySource(properties = { "regards.cloud.enabled=false", "spring.flyway.enabled=false" })
public abstract class AbstractRegardsServiceIT {

    /**
     * Default user role. User {@link #getDefaultRole()} instead.
     */
    protected static final String DEFAULT_ROLE = "ROLE_DEFAULT";

    private static final String DEFAULT_USER_EMAIL = "default_user@regards.fr";

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

    @Autowired
    protected ISubscriber subscriber;

    @Value("${regards.tenant:PROJECT}")
    private String defaultTenant;

    @Autowired
    private ApplicationEventPublisher springPublisher;

    @Before
    public void beforeITTest() {
        this.simulateApplicationReadyEvent();
        this.simulateApplicationStartedEvent();
    }

    @After
    public void afterITTest() {
        subscriber.purgeAllQueues(getDefaultTenant());
        subscriber.unsubscribeFromAll(true);
    }

    /**
     * @return class logger instance
     */
    protected Logger getLogger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Generate token for default tenant
     *
     * @param name user name
     * @param role user role
     * @return JWT
     */
    protected String generateToken(String name, final String role) {
        return jwtService.generateToken(getDefaultTenant(), name, name, role);
    }

    /**
     * Useful class to simulate a Spring Boot {@link ApplicationReadyEvent}.<br/>
     * <b>Warning : subscribers may manipulate tenant so call this method before all others.</b>
     */
    protected void simulateApplicationReadyEvent() {
        springPublisher.publishEvent(new ApplicationReadyEvent(Mockito.mock(SpringApplication.class), null, null));
    }

    /**
     * Useful class to simulate a Spring Boot {@link ApplicationStartedEvent}.<br/>
     * <b>Warning : subscribers may manipulate tenant so call this method before all others.</b>
     */
    protected void simulateApplicationStartedEvent() {
        springPublisher.publishEvent(new ApplicationStartedEvent(Mockito.mock(SpringApplication.class), null, null));
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
