/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.event.transactional;

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
import fr.cnes.regards.framework.amqp.test.event.PollEvent;

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
