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
package fr.cnes.regards.framework.jpa.multitenant.test;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.transaction.BeforeTransaction;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * Multitenant test utility class for testing service layer. This class starts up an integration test context enabling
 * auto configuration according to all available starters.<br/>
 * <br/>
 * <b>Warning : this context does not manage Spring Boot Application events (i.e. events starting with "Application"
 * like
 * {@link ApplicationReadyEvent}! You have to simulate them programmatically using
 * {@link #simulateApplicationReadyEvent()}!</b>
 * <br/>
 * <br/>
 * Warning : <b>all configuration files in the classpath will be detected and used. Add them carefully!</b>
 * <br/>
 * <br/>
 * If you do not annotate your test class with {@link MultitenantTransactional}, you will have to force the tenant
 * before you database accesses or before a potential transaction. See {@link #beforeTransaction()} implementation to
 * know how to do.
 * <br/>
 * <br/>
 * As the test uses Flyway to manage database, you have to define the target schema (<b>in lower case</b>) on your test
 * class. Use {@link TestPropertySource} to do this:
 * <br/>
 * <br/>
 * <code>
 * &#64;TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=you_schema_in_lower_case" })
 * </code>
 * <br/>
 * <br/>
 * The test is launched enabling auto configuration and scanning all components inside
 * <code>fr.cnes.regards.modules</code> base package.
 * @author Marc Sordi
 */
@ContextConfiguration(classes = { AbstractMultitenantServiceTest.ScanningConfiguration.class })
public abstract class AbstractMultitenantServiceTest extends AbstractDaoTest {

    @Autowired
    private ApplicationEventPublisher springPublisher;

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.modules" })
    public static class ScanningConfiguration {

    }

    /**
     * If inheriting class is annotated with transactional, the default tenant will be automatically injected<br/>
     * The method simply uses an {@link Autowired} {@link IRuntimeTenantResolver} to force the tenant on the current
     * thread.
     */
    @BeforeTransaction
    public void beforeTransaction() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    /**
     * Useful class to simulate a Spring Boot {@link ApplicationReadyEvent}.<br/>
     * <b>Warning : subscribers may manipulate tenant so call this method before all others.</b>
     */
    protected void simulateApplicationReadyEvent() {
        springPublisher.publishEvent(new ApplicationReadyEvent(Mockito.mock(SpringApplication.class), null, null));
    }
}
