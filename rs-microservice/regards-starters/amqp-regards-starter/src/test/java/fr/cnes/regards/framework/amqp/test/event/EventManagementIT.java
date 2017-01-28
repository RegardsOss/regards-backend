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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.MultitenantAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.AmqpTestsConfiguration;
import fr.cnes.regards.framework.amqp.test.Application;

/**
 * @author Marc Sordi
 *
 */
@ActiveProfiles("rabbit")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AmqpTestsConfiguration.class })
@SpringBootTest(classes = Application.class)
@DirtiesContext
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
     * Bean to declare exchanges, queues, bindings, ...
     */
    @Autowired
    private MultitenantAmqpAdmin regardsAmqpAdmin;

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
        TenantWrapper<PollOneMicroserviceEvent> wrapper = poller.poll(tenant, PollOneMicroserviceEvent.class);
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
        TenantWrapper<PollOneAllEvent> wrapper = poller.poll(tenant, PollOneAllEvent.class);
        PollOneAllEvent received = wrapper.getContent();
        Assert.assertEquals(message, received.getMessage());
    }

    @Test
    public void pollOneAllEventWithError() {
        // Publish a pollable event
        PollOneAllEvent event = new PollOneAllEvent();
        String message = "Poll by all!";
        event.setMessage(message);
        publisher.publish(event);

        PollableService<PollOneAllEvent> crasher = new PollableService<>(poller, true);
        try {
            crasher.pollAndSave(tenant, PollOneAllEvent.class);
        } catch (Exception e) {
            LOGGER.debug("Poll and save fails");
        }

        // Re-Poll event
        PollableService<PollOneAllEvent> winner = new PollableService<>(poller, false);
        TenantWrapper<PollOneAllEvent> wrapper = winner.pollAndSave(tenant, PollOneAllEvent.class);
        PollOneAllEvent received = wrapper.getContent();
        Assert.assertNotNull(wrapper);
        Assert.assertEquals(message, received.getMessage());

    }

    public void pollWithError() {
        // Poll event
        TenantWrapper<PollOneAllEvent> wrapper = poller.poll(tenant, PollOneAllEvent.class);
        Assert.assertNotNull(wrapper);
        throw new UnsupportedOperationException("Simulate poll error");
    }

    private void pollWithSuccess(String pMessage) {
        // Poll event
        TenantWrapper<PollOneAllEvent> wrapper = poller.poll(tenant, PollOneAllEvent.class);
        PollOneAllEvent received = wrapper.getContent();
        Assert.assertEquals(pMessage, received.getMessage());
    }

    @Test
    public void pollEachMicroserviceEvent() {
        // Publish a pollable event
        PollEachMicroserviceEvent event = new PollEachMicroserviceEvent();
        String message = "Poll by each instance of a microservice!";
        event.setMessage(message);
        publisher.publish(event);

        // This event may be polled many times
        int iter = 3;
        for (int i = 0; i < iter; i++) {
            TenantWrapper<PollEachMicroserviceEvent> wrapper = poller.poll(tenant, PollEachMicroserviceEvent.class);
            PollEachMicroserviceEvent received = wrapper.getContent();
            Assert.assertEquals(message, received.getMessage());
        }

    }
}
