/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.event;

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

/**
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { EventTestConfiguration.class })
public class TransactionalTestIT {

    /**
     * LOGGER used to populate logs when needed
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(EventManagementIT.class);

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
    private PollableServiceBean pollService;

    /**
     * Transactional publish service
     */
    @Autowired
    private PublishService publishService;

    @Test
    public void pollOneAllEventWithError() {
        // Publish a pollable event
        PollOneAllEvent event = new PollOneAllEvent();
        String message = "Poll by all!";
        event.setMessage(message);
        publisher.publish(event);

        try {
            // Bind the connection to the right vHost (i.e. tenant to publish the message)
            // poller.bind(tenant);
            pollService.pollAndSave(PollOneAllEvent.class, true);
        } catch (PollableException e) {
            LOGGER.debug(e.getMessage(), e);
        } finally {
            // poller.unbind();
        }

        // Re-Poll event
        try {
            // Bind the connection to the right vHost (i.e. tenant to publish the message)
            // poller.bind(tenant);
            TenantWrapper<PollOneAllEvent> wrapper = pollService.pollAndSave(PollOneAllEvent.class, false);
            PollOneAllEvent received = wrapper.getContent();
            Assert.assertNotNull(wrapper);
            Assert.assertEquals(message, received.getMessage());
        } catch (PollableException e) {
            LOGGER.debug(e.getMessage());
            Assert.fail();
        } finally {
            // poller.unbind();
        }
    }

    @Test
    public void publishInTransaction() {

        // Publish in transaction
        publishService.doSomethingInTransaction();

        // Poll in transaction
        try {
            // Bind the connection to the right vHost (i.e. tenant to publish the message)
            // poller.bind(tenant);
            pollService.pollAndSave(PollOneAllEvent.class, false);
        } catch (PollableException e) {
            LOGGER.debug(e.getMessage());
        } finally {
            // poller.unbind();
        }
    }
}
