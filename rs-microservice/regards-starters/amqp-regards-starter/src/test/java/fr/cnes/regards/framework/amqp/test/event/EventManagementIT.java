/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.event;

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
import fr.cnes.regards.framework.amqp.test.AmqpTestsConfiguration;

/**
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AmqpTestsConfiguration.class })
public class EventManagementIT {

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

    @Before
    public void init() throws RabbitMQVhostException {
        Assume.assumeTrue(rabbitVirtualHostAdmin.brokerRunning());
        rabbitVirtualHostAdmin.addVhost(tenant);
    }

    @Test
    public void publishAll() throws InterruptedException {
        subscriber.subscribeTo(PublishToAllEvent.class, new PublishToAllHandler());

        PublishToAllEvent event = new PublishToAllEvent();
        event.setMessage("Publish all! (i.e. broadcast)");
        eventReceived = false; // Mutated by handler
        publisher.publish(event);
        Thread.sleep(2000);
        Assert.assertTrue(eventReceived);
    }

    private class PublishToAllHandler implements IHandler<PublishToAllEvent> {

        @Override
        public void handle(TenantWrapper<PublishToAllEvent> pWrapper) {
            eventReceived = true;
            Assert.assertNotNull(pWrapper);
            LOGGER.info("Tenant : {}", pWrapper.getTenant());
            Assert.assertNotNull(pWrapper.getContent());
            String message = pWrapper.getContent().getMessage();
            Assert.assertNotNull(message);
            LOGGER.info("Handled message : {}", message);
        }
    }

    @Test
    public void pollOneMicroserviceEvent() {
        // Publish a pollable event
        PollOneMicroserviceEvent event = new PollOneMicroserviceEvent();
        String message = "Poll by one instance of a microservice!";
        event.setMessage(message);
        publisher.publish(event);

        // Poll event
        TenantWrapper<PollOneMicroserviceEvent> wrapper = poller.poll(PollOneMicroserviceEvent.class);
        PollOneMicroserviceEvent received = wrapper.getContent();
        Assert.assertEquals(message, received.getMessage());

    }

    @Test
    public void pollOneAllEvent() {
        // Publish a pollable event
        PollOneAllEvent event = new PollOneAllEvent();
        String message = "Poll by all!";
        event.setMessage(message);
        publisher.publish(event);

        // Poll event
        TenantWrapper<PollOneAllEvent> wrapper = poller.poll(PollOneAllEvent.class);
        PollOneAllEvent received = wrapper.getContent();
        Assert.assertEquals(message, received.getMessage());
    }
}
