/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.core.amqp.domain.TenantWrapper;
import fr.cnes.regards.modules.core.amqp.domain.TestEvent;
import fr.cnes.regards.modules.core.amqp.domain.TestReceiver;
import fr.cnes.regards.modules.core.exception.AddingRabbitMQVhostException;
import fr.cnes.regards.modules.core.exception.AddingRabbitMQVhostPermissionException;

/*
 * LICENSE_PLACEHOLDER
 */

/**
 * @author svissier
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AmqpTestsConfiguration.class })
@SpringBootTest(classes = ApplicationTest.class)
@DirtiesContext
public class SubscriberIT {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberIT.class);

    /**
     * PROJECT1
     */
    private static final String TENANT = "PROJECT1";

    /**
     * 2000
     */
    private static final int SLEEP_TIME = 2000;

    /**
     * SLEEP_FAIL
     */
    private static final String SLEEP_FAIL = "Sleep Failed";

    /**
     * bean to test
     */
    @Autowired
    private Subscriber subscriber;

    /**
     * bean provided
     */
    @Autowired
    private ConnectionFactory connectionFactory;

    /**
     * message received
     */
    private TestEvent received;

    /**
     * message receiver
     */
    private TestReceiver receiver;

    /**
     * Template to send original message
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Before
    public void init() {
        receiver = new TestReceiver();
        try {
            subscriber.subscribeTo(TestEvent.class, receiver, connectionFactory);
        } catch (AddingRabbitMQVhostException | AddingRabbitMQVhostPermissionException e) {
            LOGGER.error(e.getMessage(), e);
            Assert.fail("Error during init");
        }
    }

    /**
     * test the subscription to message broker
     */
    @Requirement("REGARDS_DSL_CMP_ARC_160")
    @Purpose("test the subscription to message broker")
    @Test
    public void testSubscribeTo() {
        final TestEvent toSend = new TestEvent("test2");
        final TenantWrapper<TestEvent> sended = new TenantWrapper<TestEvent>(toSend, TENANT);
        LOGGER.info("SENDED " + sended);
        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), TENANT);
        rabbitTemplate.convertAndSend(TestEvent.class.getName(), "", sended);
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());

        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            LOGGER.error(SLEEP_FAIL, e);
            Assert.fail(SLEEP_FAIL);
        }
        LOGGER.info("=================RECEIVED " + receiver.getMessage());
        received = receiver.getMessage();
        Assert.assertEquals(toSend, received);

    }

}
