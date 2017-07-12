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
package fr.cnes.regards.framework.amqp.test.event.basic;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.event.PollEvent;
import fr.cnes.regards.framework.amqp.test.event.PollMicroserviceEvent;
import fr.cnes.regards.framework.amqp.test.event.PublishEvent;

/**
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { BasicTestConfiguration.class })
public class BasicTestIT {

    /**
     * LOGGER used to populate logs when needed
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicTestIT.class);

    /**
     * Static default tenant
     */
    @Value("${regards.tenant}")
    private String tenant;

    /**
     * Bean allowing us to know if the broker is running
     */
    @Autowired
    private IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    /**
     * Publisher
     */
    @Autowired
    private IPublisher publisher;

    /**
     * Subscriber
     */
    @Autowired
    private ISubscriber subscriber;

    /**
     * Poller
     */
    @Autowired
    private IPoller poller;

    private boolean eventReceived;

    private boolean concurrentEventReceived;

    @Before
    public void init() throws RabbitMQVhostException {
        Assume.assumeTrue(rabbitVirtualHostAdmin.brokerRunning());
        rabbitVirtualHostAdmin.addVhost(tenant);
    }

    @Test
    public void publishEvent() throws InterruptedException {
        subscriber.subscribeTo(PublishEvent.class, new PublishEventHandler());
        subscriber.subscribeTo(PublishEvent.class, new ConcurrentPublishEventHandler());

        PublishEvent event = new PublishEvent();
        event.setMessage("Publish all! (i.e. broadcast)");
        eventReceived = false; // Mutated by handler
        concurrentEventReceived = false; // Mutated by handler
        publisher.publish(event);
        Thread.sleep(2000);
        Assert.assertTrue(eventReceived);
        Assert.assertTrue(concurrentEventReceived);
    }

    private class PublishEventHandler implements IHandler<PublishEvent> {

        @Override
        public void handle(TenantWrapper<PublishEvent> pWrapper) {
            eventReceived = true;
            Assert.assertNotNull(pWrapper);
            LOGGER.info("Tenant : {}", pWrapper.getTenant());
            Assert.assertNotNull(pWrapper.getContent());
            String message = pWrapper.getContent().getMessage();
            Assert.assertNotNull(message);
            LOGGER.info("Handled message : {}", message);
        }
    }

    private class ConcurrentPublishEventHandler implements IHandler<PublishEvent> {

        @Override
        public void handle(TenantWrapper<PublishEvent> pWrapper) {
            concurrentEventReceived = true;
            LOGGER.info("Concurrent handler reached");
        }

    }

    @Test
    public void pollMicroserviceEvent() {
        // Publish a pollable event
        PollMicroserviceEvent event = new PollMicroserviceEvent();
        String message = "Poll by one instance of a microservice!";
        event.setMessage(message);
        publisher.publish(event);

        // Poll event
        TenantWrapper<PollMicroserviceEvent> wrapper = poller.poll(PollMicroserviceEvent.class);
        PollMicroserviceEvent received = wrapper.getContent();
        Assert.assertEquals(message, received.getMessage());

    }

    @Test
    public void pollEvent() {
        // Publish a pollable event
        PollEvent event = new PollEvent();
        String message = "Poll by all!";
        event.setMessage(message);
        publisher.publish(event);

        // Poll event
        TenantWrapper<PollEvent> wrapper = poller.poll(PollEvent.class);
        PollEvent received = wrapper.getContent();
        Assert.assertEquals(message, received.getMessage());
    }
}
