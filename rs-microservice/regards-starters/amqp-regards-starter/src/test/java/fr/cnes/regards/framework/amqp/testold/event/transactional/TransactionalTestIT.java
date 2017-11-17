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
package fr.cnes.regards.framework.amqp.testold.event.transactional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.testold.event.PollEvent;

/**
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { TransactionalTestConfiguration.class })
public class TransactionalTestIT {

    /**
     * LOGGER used to populate logs when needed
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalTestIT.class);

    /**
     * Static default tenant
     */
    @Value("${regards.tenant}")
    private String tenant;

    /**
     * Publisher
     */
    @Autowired
    private IPublisher publisher;

    /**
     * Transactional poll service
     */
    @Autowired
    private PollableService pollService;

    /**
     * Transactional publish service
     */
    @Autowired
    private PublishService publishService;

    /**
     * Test whether polling can be done safely in transaction guaranteeing that a message isn't lost if the transaction
     * failed.
     *
     * @throws PollableException
     */
    @Test
    public void transactionalPoll() {

        // Publish a message
        PollEvent event = new PollEvent();
        String message = "Transactional poll";
        event.setMessage(message);
        publisher.publish(event);

        try {
            // Poll message and generate an exception
            pollService.transactionalPoll(PollEvent.class, true);
        } catch (UnsupportedOperationException e) {
            // Expected exception
            LOGGER.debug(e.getMessage(), e);
            // Message in unacked on the broker
        }

        // Poll message without error
        TenantWrapper<PollEvent> wrapper = pollService.transactionalPoll(PollEvent.class, false);
        // Expected message is retrieved and ack on the broker
        Assert.assertNotNull(wrapper);
        PollEvent received = wrapper.getContent();
        Assert.assertEquals(message, received.getMessage());

        // Re-poll message without error
        wrapper = pollService.transactionalPoll(PollEvent.class, false);
        // No more message on the broker : ack has run properly
        Assert.assertNull(wrapper);
    }

    /**
     * Test whether publishing can be done in transaction. If error occurs, no message is published.
     *
     * @throws PollableException
     */
    @Test
    public void transactionalPublish() {

        // Init message
        PollEvent event = new PollEvent();
        String message = "Transactional publish";
        event.setMessage(message);

        try {
            // Publish in transaction with error
            publishService.transactionalPublish(event, true);
        } catch (UnsupportedOperationException e) {
            // Expected exception
            LOGGER.debug(e.getMessage(), e);
            // Message is not published on the broker
        }

        // Poll message without error
        TenantWrapper<PollEvent> wrapper = pollService.transactionalPoll(PollEvent.class, false);
        // Verify no message was published
        Assert.assertNull(wrapper);

        // Publish in transaction without error
        publishService.transactionalPublish(event, false);

        // Poll message without error
        wrapper = pollService.transactionalPoll(PollEvent.class, false);
        // Expected message is retrieved and ack on the broker
        Assert.assertNotNull(wrapper);
        PollEvent received = wrapper.getContent();
        Assert.assertEquals(message, received.getMessage());
    }
}
