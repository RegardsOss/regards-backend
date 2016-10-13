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
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;
import fr.cnes.regards.modules.core.amqp.domain.TestEvent;
import fr.cnes.regards.modules.core.amqp.domain.TestReceiver;
import fr.cnes.regards.modules.core.amqp.utils.TenantWrapper;
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
@ContextConfiguration(classes = { SubscriberTestsConfiguration.class })
@SpringBootTest(classes = ApplicationTest.class)
@DirtiesContext
public class SubscriberIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberIT.class);

    @Autowired
    private Subscriber subscriber;

    @Autowired
    private ConnectionFactory connectionFactory;

    private TestEvent received;

    private TestReceiver receiver;

    private SimpleMessageListenerContainer container;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Jackson2JsonMessageConverter jackson2JsonMessageConverter;

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

    @Requirement("REGARDS_DSL_CMP_ARC_160")
    @Purpose("test the subscribing to message broker")
    @Test
    public void testSubscribeTo() {
        final TestEvent toSend = new TestEvent("test2");
        final TenantWrapper<TestEvent> sended = new TenantWrapper<TestEvent>(toSend, "PROJECT1");
        LOGGER.info("SENDED " + sended);
        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), "PROJECT1");
        rabbitTemplate.convertAndSend("REGARDS", TestEvent.class.getName(), sended);
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            LOGGER.error("Sleep Failed", e);
            Assert.fail("Sleep Failed");
        }
        LOGGER.info("=================RECEIVED " + receiver.getMessage());
        received = receiver.getMessage();
        Assert.assertEquals(sended, received);

    }

}
