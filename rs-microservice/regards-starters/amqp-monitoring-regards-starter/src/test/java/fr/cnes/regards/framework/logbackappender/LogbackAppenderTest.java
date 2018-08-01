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
package fr.cnes.regards.framework.logbackappender;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.authentication.autoconfigure.AuthenticationAutoConfiguration;
import fr.cnes.regards.framework.logbackappender.domain.LogEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@ContextConfiguration(classes = { DefaultTestConfiguration.class })
public class LogbackAppenderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogbackAppenderTest.class);

    private static final int SLEEP_TIME = 500;

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
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ITenantResolver tenantResolver;

    /**
     * bean allowing us to know if the broker is running
     */
    @Autowired
    private IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    @Autowired
    private SubscriberLogEvent receiverLogEvent;

    @Before
    public void init() {
        // Get all tenants
        tenants = Lists.newArrayList(tenantResolver.getAllTenants());

        defaultTenant = tenants.get(0);
        otherTenant = tenants.get(1);

        // initialize the runtime tenant for the message to publish
        runtimeTenantResolver.forceTenant(defaultTenant);

        rabbitVirtualHostAdmin.addVhost(RabbitVirtualHostAdmin.getVhostName(defaultTenant));
        rabbitVirtualHostAdmin.addVhost(RabbitVirtualHostAdmin.getVhostName(otherTenant));

        // before each test, reset the LogEvent received
        receiverLogEvent.reset();
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_ARC_100")
    @Purpose("Send log as event for monitoring purpose")
    public void getOneLogInfo() throws InterruptedException {
        final String message = "Hello info message : " + defaultTenant;
        LOGGER.info(message);
        LOGGER.debug("Hello");

        // wait and get all the received log events
        Thread.sleep(SLEEP_TIME); // NOSONAR : passive test so we have to wait for the message to be sent

        final List<TenantWrapper<LogEvent>> wrapperReceivedEvents = receiverLogEvent.getMessages();
        Assert.assertNotNull(wrapperReceivedEvents);
        Assert.assertTrue(!wrapperReceivedEvents.isEmpty());

        // control the uniq log event received
        Assert.assertEquals(1, wrapperReceivedEvents.size());

        final TenantWrapper<LogEvent> wrapperReceived = wrapperReceivedEvents.get(0);
        Assert.assertNotNull(wrapperReceived);
        final LogEvent received = wrapperReceived.getContent();
        controlLogEvent(received);
        Assert.assertEquals(message, received.getMsg());
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_ARC_100")
    @Purpose("Send log as event for monitoring purpose")
    public void getManyLogInfo() throws InterruptedException {
        LOGGER.info("Hello info message : {}", defaultTenant);
        LOGGER.info("Hello another info message : {}", defaultTenant);
        LOGGER.info("Hello third info message : {}", defaultTenant);

        LOGGER.debug("Hello");

        // wait and get all the received log events
        Thread.sleep(SLEEP_TIME); // NOSONAR : passive test so we have to wait for the message to be sent

        final List<TenantWrapper<LogEvent>> wrapperReceivedEvents = receiverLogEvent.getMessages();
        Assert.assertNotNull(wrapperReceivedEvents);
        Assert.assertTrue(!wrapperReceivedEvents.isEmpty());

        // control the log event
        Assert.assertEquals(3, wrapperReceivedEvents.size());

        // first event
        TenantWrapper<LogEvent> wrapperReceived = wrapperReceivedEvents.get(0);
        Assert.assertNotNull(wrapperReceived);
        LogEvent received = wrapperReceived.getContent();
        controlLogEvent(received);
        Assert.assertEquals(Thread.currentThread().getStackTrace()[1].getMethodName(), received.getMethod());

        // second event
        wrapperReceived = wrapperReceivedEvents.get(1);
        Assert.assertNotNull(wrapperReceived);
        received = wrapperReceived.getContent();
        Assert.assertNotNull(received);
        controlLogEvent(received);
        Assert.assertEquals(Thread.currentThread().getStackTrace()[1].getMethodName(), received.getMethod());

        // third event
        wrapperReceived = wrapperReceivedEvents.get(2);
        Assert.assertNotNull(wrapperReceived);
        received = wrapperReceived.getContent();
        controlLogEvent(received);
        Assert.assertEquals(Thread.currentThread().getStackTrace()[1].getMethodName(), received.getMethod());
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_ARC_100")
    @Purpose("Send log as event for monitoring purpose")
    public void getNoLogInfo() throws InterruptedException {
        LOGGER.debug("Hello");

        // wait and get all the received log events
        Thread.sleep(SLEEP_TIME); // NOSONAR : passive test so we have to wait for the message to be sent

        final List<TenantWrapper<LogEvent>> wrapperReceivedEvents = receiverLogEvent.getMessages();
        Assert.assertNotNull(wrapperReceivedEvents);
        Assert.assertEquals(0, wrapperReceivedEvents.size());
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_ARC_100")
    @Purpose("Send log as event for monitoring purpose")
    public void getMonitoringLogEventMultiTenants() throws InterruptedException {
        final String msg2Send = "Hello I'am an event";
        LOGGER.info(msg2Send);

        runtimeTenantResolver.forceTenant(otherTenant);

        final String otherMsg2Send = "Hello I'am an other event on a different tenant";
        LOGGER.info(otherMsg2Send);

        // wait and get all the received log events
        Thread.sleep(SLEEP_TIME); // NOSONAR : passive test so we have to wait for the message to be sent

        final List<TenantWrapper<LogEvent>> wrapperReceivedEvents = receiverLogEvent.getMessages();
        Assert.assertNotNull(wrapperReceivedEvents);
        Assert.assertTrue(!wrapperReceivedEvents.isEmpty());

        // first event
        TenantWrapper<LogEvent> wrapperReceived = wrapperReceivedEvents.get(0);
        Assert.assertNotNull(wrapperReceived);
        Assert.assertEquals(wrapperReceived.getTenant(), defaultTenant);
        LogEvent received = wrapperReceived.getContent();
        controlLogEvent(received);
        Assert.assertEquals(Thread.currentThread().getStackTrace()[1].getMethodName(), received.getMethod());

        // second event
        wrapperReceived = wrapperReceivedEvents.get(1);
        Assert.assertNotNull(wrapperReceived);
        Assert.assertEquals(wrapperReceived.getTenant(), otherTenant);
        received = wrapperReceived.getContent();
        controlLogEvent(received);
        Assert.assertEquals(Thread.currentThread().getStackTrace()[1].getMethodName(), received.getMethod());
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_ARC_100")
    @Purpose("Send log as event for monitoring purpose")
    public void getMonitoringLogEvents() throws InterruptedException {
        final int n = 100;
        final String msg2Send = "Hello I'am an event";

        for (int i = 0; i < n; i++) {
            LOGGER.info(msg2Send + " - " + i);
        }

        // wait and get all the received log events
        Thread.sleep(3000); // NOSONAR : passive test so we have to wait for the message to be sent

        final List<TenantWrapper<LogEvent>> wrapperReceivedEvents = receiverLogEvent.getMessages();
        Assert.assertNotNull(wrapperReceivedEvents);
        Assert.assertTrue(!wrapperReceivedEvents.isEmpty());
        Assert.assertEquals(n, wrapperReceivedEvents.size());

        // first event
        TenantWrapper<LogEvent> wrapperReceived = wrapperReceivedEvents.get(0);
        Assert.assertNotNull(wrapperReceived);
        Assert.assertEquals(wrapperReceived.getTenant(), defaultTenant);
        LogEvent received = wrapperReceived.getContent();
        controlLogEvent(received);
        Assert.assertEquals(Thread.currentThread().getStackTrace()[1].getMethodName(), received.getMethod());
    }

    private void controlLogEvent(LogEvent event) {
        Assert.assertNotNull(event);
        Assert.assertEquals(this.getClass().getCanonicalName(), event.getCaller());
        Assert.assertEquals(microserviceName, event.getMicroservice());
        Assert.assertEquals(AuthenticationAutoConfiguration.DEFAULT_USER, event.getUserName());
    }

}
