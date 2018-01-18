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
package fr.cnes.regards.modules.metrics.service;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.metrics.dao.ILogEventRepository;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@PropertySource("classpath:amqp-rabbit.properties")
@ComponentScan(basePackages = { "fr.cnes.regards.modules.metrics.dao", "fr.cnes.regards.modules.metrics.service" })
public class LogEventTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogEventTest.class);

    private static final int SLEEP_TIME = 500;

    private static final String FIRST_MESSAGE = "Hello I'am an event";

    private static final String SECOND_MESSAGE = "Hello I'am an other event on a different defaultTenant";

    /**
     * Static default defaultTenant
     */
    private String defaultTenant;

    private String otherTenant;

    private List<String> tenants;

    /**
     * The microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    @Autowired
    ILogEventRepository logEventRepository;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Before
    public void init() {
        // Get all tenants
        tenants = Lists.newArrayList(tenantResolver.getAllTenants());

        defaultTenant = tenants.get(0);
        otherTenant = tenants.get(1);

        // delete all on the repository for the 2 tenants
        runtimeTenantResolver.forceTenant(otherTenant);
        logEventRepository.deleteAll();
        Assert.assertEquals(0, logEventRepository.count());

        runtimeTenantResolver.forceTenant(defaultTenant);
        logEventRepository.deleteAll();
        Assert.assertEquals(0, logEventRepository.count());
    }

    @Test
    public void monitorOneLogEvent() throws InterruptedException {
        runtimeTenantResolver.forceTenant(defaultTenant);

        LOGGER.info(FIRST_MESSAGE + " {}", defaultTenant);

        // wait and get all the received log events
        Thread.sleep(SLEEP_TIME); // NOSONAR : passive test so we have to wait for the message to be sent

        Assert.assertEquals(1, logEventRepository.count());
    }

    @Test
    public void monitorSeveralLogEvents() throws InterruptedException {
        runtimeTenantResolver.forceTenant(defaultTenant);
        final int n = 10;
        for (int i = 0; i < n; i++) {
            LOGGER.info(FIRST_MESSAGE + " - " + i);
        }

        // wait and get all the received log events
        Thread.sleep(3000); // NOSONAR : passive test so we have to wait for the message to be sent

        Assert.assertEquals(n, logEventRepository.count());
    }

    @Test
    public void monitorLogEventsMultiTenants() throws InterruptedException {
        runtimeTenantResolver.forceTenant(defaultTenant);
        int n = 10;
        for (int i = 0; i < n; i++) {
            LOGGER.info("[" + defaultTenant + "] " + FIRST_MESSAGE + " - " + i);
        }

        sendMessageOneTenant(otherTenant, SECOND_MESSAGE + "-" + LocalDateTime.now());

        n++;
        sendMessageOneTenant(defaultTenant, FIRST_MESSAGE + "-" + LocalDateTime.now());

        sendMessageOneTenant(otherTenant, SECOND_MESSAGE + "-" + LocalDateTime.now());

        Thread.sleep(SLEEP_TIME); // NOSONAR : passive test so we have to wait for the message to be sent

        Assert.assertEquals(2, logEventRepository.count());

        runtimeTenantResolver.forceTenant(defaultTenant);
        Assert.assertEquals(n, logEventRepository.count());
    }

    @Test
    public void monitorAlternateLogEventsMultiTenants() throws InterruptedException {
        runtimeTenantResolver.forceTenant(defaultTenant);
        int n = 30;
        for (int i = 0; i < n; i++) {
            sendMessageOneTenant(otherTenant, SECOND_MESSAGE + "-" + LocalDateTime.now() + " ---> " + i);

            sendMessageOneTenant(defaultTenant, FIRST_MESSAGE + "-" + LocalDateTime.now() + " ---> " + i);
        }

        Thread.sleep(SLEEP_TIME); // NOSONAR : passive test so we have to wait for the message to be sent

        runtimeTenantResolver.forceTenant(otherTenant);
        Assert.assertEquals(n, logEventRepository.count());

        runtimeTenantResolver.forceTenant(defaultTenant);
        Assert.assertEquals(n, logEventRepository.count());
    }

    @Test
    public void monitorLogEventsMultiTenantsOnlyOneLogForATenant() throws InterruptedException {
        runtimeTenantResolver.forceTenant(defaultTenant);
        int n = 20;
        for (int i = 0; i < n; i++) {
            sendMessageOneTenant(defaultTenant, FIRST_MESSAGE + "-" + LocalDateTime.now() + " ---> " + i);
        }

        sendMessageOneTenant(otherTenant, SECOND_MESSAGE);

        for (int i = 0; i < n; i++) {
            sendMessageOneTenant(defaultTenant, FIRST_MESSAGE + "-" + LocalDateTime.now() + " ---> " + i);
        }

        Thread.sleep(SLEEP_TIME); // NOSONAR : passive test so we have to wait for the message to be sent

        runtimeTenantResolver.forceTenant(otherTenant);
        Assert.assertEquals(1, logEventRepository.count());

        runtimeTenantResolver.forceTenant(defaultTenant);
        Assert.assertEquals(2 * n, logEventRepository.count());
    }

    private void sendMessageOneTenant(String pTenant, String pMessage) throws InterruptedException {
        runtimeTenantResolver.forceTenant(pTenant);
        LOGGER.info("[" + pTenant + "] " + pMessage);
        Thread.sleep(SLEEP_TIME);// NOSONAR : passive test so we have to wait for the message to be sent
    }

}
