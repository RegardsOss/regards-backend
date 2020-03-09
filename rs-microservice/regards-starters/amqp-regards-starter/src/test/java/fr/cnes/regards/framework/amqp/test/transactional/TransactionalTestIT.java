/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.test.transactional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.test.event.PollableInfo;

/**
 * @author Marc Sordi
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TransactionalTestConfiguration.class)
@TestPropertySource(properties = { "regards.amqp.management.mode=MULTI", "regards.tenants=PROJECT, PROJECT1",
        "regards.tenant=PROJECT", "regards.amqp.internal.transaction=true" }, locations = "classpath:amqp.properties")
public class TransactionalTestIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalTestIT.class);

    @Autowired
    private IPublisher publisher;

    /**
     * Transactional poller service
     */
    @Autowired
    private PollableService pollService;

    /**
     * Transactional publisher service
     */
    @Autowired
    private PublishService publishService;

    /**
     * Test whether polling can be done safely in transaction
     * guaranteeing that a message isn't lost if the transaction failed.
     */
    @Test
    public void transactionalPoll() {

        // Publish message
        PollableInfo info = new PollableInfo();
        String message = "Transactional poll";
        info.setMessage(message);
        publisher.publish(info, true);

        try {
            // Poll message and generate an exception
            pollService.transactionalPoll(PollableInfo.class, true);
        } catch (UnsupportedOperationException e) {
            // Expected exception
            LOGGER.debug("Polling failed ... continuing");
            // Message is unacked on the broker
        }

        // Poll message without error
        TenantWrapper<PollableInfo> wrapper = pollService.transactionalPoll(PollableInfo.class, false);
        // Expected message is retrieved and ack on the broker
        Assert.assertNotNull(wrapper);
        PollableInfo received = wrapper.getContent();
        Assert.assertEquals(message, received.getMessage());

        // Re-poll message without error
        wrapper = pollService.transactionalPoll(PollableInfo.class, false);
        // No more message on the broker : ack has run properly
        Assert.assertNull(wrapper);
    }

    /**
     * Test whether publishing can be done in transaction.
     * If error occurs, no message is published.
     */
    @Test
    public void transactionalPublish() {

        // Init message
        PollableInfo info = new PollableInfo();
        String message = "Transactional publish";
        info.setMessage(message);

        try {
            // Publish in transaction with error
            publishService.transactionalPublish(info, true, true);
        } catch (UnsupportedOperationException e) {
            // Expected exception
            LOGGER.debug("Publishing failed ... continuing");
            // Message is not published on the broker
        }

        // Poll message without error
        TenantWrapper<PollableInfo> wrapper = pollService.transactionalPoll(PollableInfo.class, false);
        // Verify no message was published
        Assert.assertNull(wrapper);

        // Publish in transaction without error
        publishService.transactionalPublish(info, false, true);

        // Poll message without error
        wrapper = pollService.transactionalPoll(PollableInfo.class, false);
        // Expected message is retrieved and ack on the broker
        Assert.assertNotNull(wrapper);
        PollableInfo received = wrapper.getContent();
        Assert.assertEquals(message, received.getMessage());
    }
}
