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
package fr.cnes.regards.framework.amqp.test.batch;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * @author Marc SORDI
 *
 */
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@TestPropertySource(properties = { "regards.amqp.management.mode=SINGLE",
        "regards.tenants=" + BatchTestIT.PROJECT + ", " + BatchTestIT.PROJECT1, "regards.tenant=" + BatchTestIT.PROJECT,
        "regards.amqp.internal.transaction=true", "spring.jmx.enabled=false" }, locations = "classpath:amqp.properties")
public class BatchTestIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchTestIT.class);

    static final String PROJECT = "PROJECT";

    static final String PROJECT1 = "PROJECT1";

    private static final Integer MESSAGE_NB_PROJECT = 10;

    private static final Integer MESSAGE_NB_PROJECT1 = 5;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    private BatchHandler batchHandler;

    @Autowired
    private IAmqpAdmin amqpAdmin;

    @Autowired
    private IRabbitVirtualHostAdmin vhostAdmin;

    @Before
    public void before() {
        // New instance for each test
        batchHandler = new BatchHandler();
        // Subscribe to message
        subscriber.subscribeTo(BatchMessage.class, batchHandler);
    }

    @After
    public void after() {
        // Unsubscribe
        subscriber.unsubscribeFrom(BatchMessage.class);

        // Purge queue
        cleanAMQPQueues(BatchHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
    }

    @Test
    public void processBatch() throws InterruptedException {

        // Publish message in default project
        for (int i = 1; i <= MESSAGE_NB_PROJECT; i++) {
            BatchMessage m = BatchMessage.build(String.format("%s_batch_0%02d", PROJECT, i));
            m.setMessageProperties(new MessageProperties());
            m.setHeader("header", "value");
            publisher.publish(m);
        }

        // Publish messages in second project
        try {
            tenantResolver.forceTenant(PROJECT1);
            for (int i = 1; i <= MESSAGE_NB_PROJECT1; i++) {
                publisher.publish(BatchMessage.build(String.format("%s_batch_1%02d", PROJECT1, i)));
            }
        } finally {
            tenantResolver.clearTenant();
        }

        Thread.sleep(5000);
        Assert.assertTrue(batchHandler.getCountByTenant(PROJECT) == MESSAGE_NB_PROJECT);
        Assert.assertTrue(batchHandler.getCountByTenant(PROJECT1) == MESSAGE_NB_PROJECT1);
        Assert.assertTrue(batchHandler.getCalls() == 2);
    }

    @Test
    public void processUnknownTenant() throws InterruptedException {

        try {
            tenantResolver.forceTenant(BatchHandler.FAKE_TENANT);
            publisher.publish(BatchMessage.build(String.format("%s_batch", BatchHandler.FAKE_TENANT)));
        } finally {
            tenantResolver.clearTenant();
        }

        try {
            tenantResolver.forceTenant(PROJECT);
            publisher.publish(BatchMessage.build(String.format("%s_batch", PROJECT)));
        } finally {
            tenantResolver.clearTenant();
        }

        Thread.sleep(5000);
        Assert.assertTrue(batchHandler.getCountByTenant(BatchHandler.FAKE_TENANT) == 0);
        Assert.assertTrue(batchHandler.getCountByTenant(PROJECT) == 1);
    }

    @Test
    public void processingFailureTest() throws InterruptedException {
        try {
            tenantResolver.forceTenant(BatchHandler.FAIL_TENANT);
            publisher.publish(BatchMessage.build(String.format("%s_batch", BatchHandler.FAIL_TENANT)));
        } finally {
            tenantResolver.clearTenant();
        }

        Thread.sleep(5000);
        Assert.assertTrue(batchHandler.getFailsByTenant(BatchHandler.FAIL_TENANT) == 0);
        Assert.assertTrue(batchHandler.getCountByTenant(BatchHandler.FAIL_TENANT) == 0);
    }

    /**
     * Internal method to clean AMQP queues, if actives
     */
    private void cleanAMQPQueues(Class<? extends IHandler<?>> handler, Target target) {
        if (vhostAdmin != null) {
            // Re-set tenant because above simulation clear it!

            // Purge event queue
            try {
                vhostAdmin.bind(AmqpConstants.AMQP_MULTITENANT_MANAGER);
                amqpAdmin.purgeQueue(amqpAdmin.getSubscriptionQueueName(handler, target), false);
            } catch (AmqpIOException e) {
                LOGGER.warn("Failed to clean AMQP queues");
            } finally {
                vhostAdmin.unbind();
            }
        }
    }

}
