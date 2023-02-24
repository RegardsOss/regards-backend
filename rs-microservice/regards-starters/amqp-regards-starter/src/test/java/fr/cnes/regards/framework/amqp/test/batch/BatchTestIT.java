/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.AmqpChannel;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * @author Marc SORDI
 */
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@TestPropertySource(properties = { "regards.amqp.management.mode=SINGLE",
                                   "regards.tenants=" + BatchTestIT.PROJECT + ", " + BatchTestIT.PROJECT1,
                                   "regards.tenant=" + BatchTestIT.PROJECT,
                                   "regards.amqp.internal.transaction=true",
                                   "spring.jmx.enabled=false" }, locations = "classpath:amqp.properties")
@ActiveProfiles("test")
public class BatchTestIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchTestIT.class);

    static final String PROJECT = "PROJECT";

    static final String PROJECT1 = "PROJECT1";

    private static final Integer MESSAGE_NB_PROJECT = 10;

    private static final Integer MESSAGE_NB_PROJECT1 = 5;

    public static final String FAKE_TENANT = "FAKE";

    @Autowired
    private IPublisher publisher;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    private BatchHandler batchHandler;

    private BatchHandlerBis batchHandlerBis;

    private BatchHandlerTers batchHandlerTers;

    @Autowired
    private IAmqpAdmin amqpAdmin;

    @Autowired
    private IRabbitVirtualHostAdmin vhostAdmin;

    private static final String QUEUE_NAME = "test.queue.name";

    private static final String EXCHANGE_NAME = "test.exchange.name";

    //    @Autowired(required = false)
    //    private List<HealthIndicator> indicators;

    @Before
    public void before() {
        // New instance for each test
        batchHandler = new BatchHandler(tenantResolver);
        batchHandlerBis = new BatchHandlerBis(tenantResolver);
        batchHandlerTers = new BatchHandlerTers(tenantResolver);
        // Subscribe to message
        subscriber.subscribeTo(BatchedMessage.class, batchHandler);
        subscriber.subscribeTo(BatchedMessage.class, batchHandlerBis);
        subscriber.subscribeTo(BatchedMessage.class, batchHandlerTers, QUEUE_NAME, EXCHANGE_NAME);
    }

    @After
    public void after() {
        // Unsubscribe
        subscriber.unsubscribeFrom(BatchedMessage.class, true);

        // Purge queue
        cleanAMQPQueues(BatchHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
        cleanAMQPQueue(QUEUE_NAME);
    }

    @Test
    public void processConversionException() throws InterruptedException {

        // Publish message in default project
        String body = "Unparseable";
        Message message = new Message(body.getBytes(), new MessageProperties());
        message.getMessageProperties().setHeader(AmqpConstants.REGARDS_CONVERTER_HEADER, JsonMessageConverter.GSON);
        message.getMessageProperties().setHeader(AmqpConstants.REGARDS_TENANT_HEADER, PROJECT);

        String exchange = amqpAdmin.getBroadcastExchangeName(BatchedMessage.class.getName(),
                                                             Target.ONE_PER_MICROSERVICE_TYPE);
        publisher.basicPublish(PROJECT, exchange, "", message);

        Thread.sleep(5000);
        Assert.assertTrue(batchHandler.getCountByTenant(PROJECT) == 0);
        Assert.assertTrue(batchHandler.getCalls() == 0);
        Assert.assertTrue(batchHandler.getInvalidByTenant(PROJECT) == 0);
    }

    @Test
    public void processValidBatch() throws InterruptedException {

        // Publish message in default project
        for (int i = 1; i <= MESSAGE_NB_PROJECT; i++) {
            BatchedMessage m = BatchedMessage.build(BatchHandler.VALID);
            m.setMessageProperties(new MessageProperties());
            m.setHeader("header", "value");
            publisher.publish(m);
        }

        // Publish messages in second project
        try {
            tenantResolver.forceTenant(PROJECT1);
            for (int i = 1; i <= MESSAGE_NB_PROJECT1; i++) {
                publisher.publish(BatchedMessage.build(BatchHandler.VALID));
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
    public void processValidBatchOnNamedQueue() throws InterruptedException {

        List<BatchedMessage> messages = new ArrayList<>();

        // Publish message in default project
        for (int i = 1; i <= MESSAGE_NB_PROJECT; i++) {
            BatchedMessage m = BatchedMessage.build(BatchHandler.VALID);
            m.setMessageProperties(new MessageProperties());
            m.setHeader("header", "value");
            messages.add(m);
        }
        try {
            tenantResolver.forceTenant(PROJECT);
            publisher.broadcastAll(EXCHANGE_NAME,
                                   Optional.empty(),
                                   Optional.empty(),
                                   Optional.empty(),
                                   0,
                                   messages,
                                   new HashMap<>());
        } finally {
            tenantResolver.clearTenant();
        }

        messages.clear();
        // Publish message in default project
        for (int i = 1; i <= MESSAGE_NB_PROJECT1; i++) {
            BatchedMessage m = BatchedMessage.build(BatchHandler.VALID);
            m.setMessageProperties(new MessageProperties());
            m.setHeader("header", "value");
            messages.add(m);
        }
        try {
            tenantResolver.forceTenant(PROJECT1);
            publisher.broadcastAll(EXCHANGE_NAME,
                                   Optional.empty(),
                                   Optional.empty(),
                                   Optional.empty(),
                                   0,
                                   messages,
                                   new HashMap<>());
        } finally {
            tenantResolver.clearTenant();
        }
        Thread.sleep(5000);
        Assert.assertEquals("Invalid number of message received",
                            MESSAGE_NB_PROJECT,
                            batchHandlerTers.getCountByTenant(PROJECT));
        Assert.assertEquals("Invalid number of message received",
                            MESSAGE_NB_PROJECT1,
                            batchHandlerTers.getCountByTenant(PROJECT1));
        Assert.assertEquals("Invalid number of calls", 2, batchHandlerTers.getCalls().intValue());
    }

    @Test
    public void processInvalidMessage() throws InterruptedException {

        // Publish message in default project
        BatchedMessage m = BatchedMessage.build(BatchHandler.INVALID);
        m.setMessageProperties(new MessageProperties());
        m.setHeader("header", "value");
        publisher.publish(m);

        Thread.sleep(5000);
        Assert.assertTrue(batchHandler.getCountByTenant(PROJECT) == 0);
        Assert.assertTrue(batchHandler.getCalls() == 0);
        Assert.assertTrue(batchHandler.getInvalidByTenant(PROJECT) == 1);
    }

    @Test
    public void processInvalidAndValidMessages() throws InterruptedException {

        // Publish INVALID message in default project
        BatchedMessage m = BatchedMessage.build(BatchHandler.INVALID);
        m.setMessageProperties(new MessageProperties());
        m.setHeader("header", "value");
        publisher.publish(m);

        // Publish VALID message in default project
        BatchedMessage valid = BatchedMessage.build(BatchHandler.VALID);
        publisher.publish(valid);

        Thread.sleep(5000);
        Assert.assertTrue(batchHandler.getCountByTenant(PROJECT) == 1);
        Assert.assertTrue(batchHandler.getCalls() == 1);
        Assert.assertTrue(batchHandler.getInvalidByTenant(PROJECT) == 1);
    }

    @Test
    public void processBatchException() throws InterruptedException {

        // Publish message in default project
        BatchedMessage m = BatchedMessage.build(BatchHandler.THROW_EXCEPTION);
        m.setMessageProperties(new MessageProperties());
        m.setHeader("header", "value");
        publisher.publish(m);

        Thread.sleep(5000);
        Assert.assertTrue(batchHandler.getCountByTenant(PROJECT) == 0);
        Assert.assertTrue(batchHandler.getCalls() == 1);
        Assert.assertTrue(batchHandler.getInvalidByTenant(PROJECT) == 0);
        Assert.assertTrue(batchHandler.getFailsByTenant(PROJECT) == 1);
    }

    @Test
    public void processUnknownTenant() throws InterruptedException {

        try {
            tenantResolver.forceTenant(FAKE_TENANT);
            publisher.publish(BatchedMessage.build(BatchHandler.VALID));
        } finally {
            tenantResolver.clearTenant();
        }

        try {
            tenantResolver.forceTenant(PROJECT);
            publisher.publish(BatchedMessage.build(BatchHandler.VALID));
        } finally {
            tenantResolver.clearTenant();
        }

        Thread.sleep(5000);
        Assert.assertTrue(batchHandler.getCountByTenant(FAKE_TENANT) == 0);
        Assert.assertTrue(batchHandler.getCountByTenant(PROJECT) == 1);
    }

    /**
     * Internal method to clean AMQP queues, if actives
     */
    private void cleanAMQPQueues(Class<? extends IHandler<?>> handler, Target target) {
        if (vhostAdmin != null) {
            // Re-set tenant because above simulation clear it!

            // Purge event queue
            try {
                vhostAdmin.bind(AmqpChannel.AMQP_MULTITENANT_MANAGER);
                amqpAdmin.purgeQueue(amqpAdmin.getSubscriptionQueueName(handler, target), false);
            } catch (AmqpIOException e) {
                LOGGER.warn("Failed to clean AMQP queues");
            } finally {
                vhostAdmin.unbind();
            }
        }
    }

    private void cleanAMQPQueue(String queueName) {
        if (vhostAdmin != null) {
            // Re-set tenant because above simulation clear it!

            // Purge event queue
            try {
                vhostAdmin.bind(AmqpChannel.AMQP_MULTITENANT_MANAGER);
                amqpAdmin.purgeQueue(queueName, false);
            } catch (AmqpIOException e) {
                LOGGER.warn("Failed to clean AMQP queues");
            } finally {
                vhostAdmin.unbind();
            }
        }
    }

}
