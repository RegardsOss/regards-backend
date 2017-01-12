/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test;

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
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.event.PublishAllEvent;
import fr.cnes.regards.framework.amqp.utils.IRabbitVirtualHostUtils;

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
    private IRabbitVirtualHostUtils rabbitVirtualHostUtils;

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

    @Before
    public void init() throws RabbitMQVhostException {
        Assume.assumeTrue(rabbitVirtualHostUtils.brokerRunning());
        rabbitVirtualHostUtils.addVhost(tenant);
    }

    @Test
    public void publishAll() throws InterruptedException {
        subscriber.subscribeTo(PublishAllEvent.class, new PublishAllHandler());

        PublishAllEvent event = new PublishAllEvent();
        event.setMessage("Publish all! (i.e. broadcast)");
        publisher.publish(event);

        Thread.sleep(5);
    }

    private class PublishAllHandler implements IHandler<PublishAllEvent> {

        @Override
        public void handle(TenantWrapper<PublishAllEvent> pWrapper) {
            Assert.assertNotNull(pWrapper);
            LOGGER.info("Tenant : {}", pWrapper.getTenant());
            Assert.assertNotNull(pWrapper.getContent());
            String message = pWrapper.getContent().getMessage();
            Assert.assertNotNull(message);
            LOGGER.info("Handled message : {}", message);
        }
    }
}
