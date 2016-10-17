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
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.core.amqp.configuration.AmqpConfiguration;
import fr.cnes.regards.modules.core.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.modules.core.amqp.domain.TenantWrapper;
import fr.cnes.regards.modules.core.amqp.domain.TestEvent;
import fr.cnes.regards.modules.core.exception.AddingRabbitMQVhostException;
import fr.cnes.regards.modules.core.exception.AddingRabbitMQVhostPermissionException;

/**
 * @author svissier
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AmqpTestsConfiguration.class })
@SpringBootTest(classes = ApplicationTest.class)
@DirtiesContext
public class PollerIT {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PollerIT.class);

    /**
     * PROJECT1
     */
    private static final String TENANT = "PROJECT_POLLER_IT";

    /**
     * 2000
     */
    private static final int SLEEP_TIME = 2000;

    /**
     * SLEEP_FAIL
     */
    private static final String SLEEP_FAIL = "Sleep Failed";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Poller poller;

    @Autowired
    private AmqpConfiguration amqpConfiguration;

    @Before
    public void init() throws AddingRabbitMQVhostException, AddingRabbitMQVhostPermissionException {
        amqpConfiguration.addVhost(TENANT);
        final Exchange exchange = amqpConfiguration.declareExchange(TestEvent.class.getName(),
                                                                    AmqpCommunicationMode.ONE_TO_MANY, TENANT);
        final Queue queue = amqpConfiguration.declarequeue(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT);
        amqpConfiguration.declareBinding(queue, exchange, "", AmqpCommunicationMode.ONE_TO_MANY, TENANT);

        final Exchange exchangeOneToOne = amqpConfiguration.declareExchange(TestEvent.class.getName(),
                                                                            AmqpCommunicationMode.ONE_TO_ONE, TENANT);
        final Queue queueOneToOne = amqpConfiguration.declarequeue(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE,
                                                                   TENANT);
        amqpConfiguration.declareBinding(queueOneToOne, exchangeOneToOne, TestEvent.class.getName(),
                                         AmqpCommunicationMode.ONE_TO_ONE, TENANT);
    }

    /**
     * test the polling ONE_TO_MANY to message broker
     */
    @Requirement("REGARDS_DSL_CMP_ARC_170")
    @Purpose("test the polling ONE_TO_MANY to message broker")
    @Test
    public void testPollOneToMany() {
        final TestEvent toSend = new TestEvent("test3");
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
        final TenantWrapper<TestEvent> wrapperReceived;
        try {
            wrapperReceived = poller.poll(TENANT, TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY);
            LOGGER.info("=================RECEIVED " + wrapperReceived.getContent());
            final TestEvent received = wrapperReceived.getContent();
            Assert.assertEquals(toSend, received);
        } catch (AddingRabbitMQVhostException | AddingRabbitMQVhostPermissionException e) {
            final String msg = "Polling one to many Test Failed";
            LOGGER.error(msg, e);
            Assert.fail(msg);
        }

    }

    /**
     * test the polling ONE_TO_ONE to message broker
     */
    @Requirement("REGARDS_DSL_CMP_ARC_170")
    @Purpose("test the polling ONE_TO_ONE to message broker")
    @Test
    public void testPollOneToOne() {
        final TestEvent toSend = new TestEvent("test4");
        final TenantWrapper<TestEvent> sended = new TenantWrapper<TestEvent>(toSend, TENANT);
        LOGGER.info("SENDED :" + sended);
        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), TENANT);
        // for one to one communication, exchange is named after the application and not what we are exchanging
        rabbitTemplate.convertAndSend("REGARDS", TestEvent.class.getName(), sended);
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());

        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            LOGGER.error(SLEEP_FAIL, e);
            Assert.fail(SLEEP_FAIL);
        }
        final TenantWrapper<TestEvent> wrapperReceived;
        try {
            wrapperReceived = poller.poll(TENANT, TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE);
            LOGGER.info("=================RECEIVED :" + wrapperReceived.getContent());
            final TestEvent received = wrapperReceived.getContent();
            Assert.assertEquals(toSend, received);
        } catch (AddingRabbitMQVhostException | AddingRabbitMQVhostPermissionException e) {
            final String msg = "Polling one to one Test Failed";
            LOGGER.error(msg, e);
            Assert.fail(msg);
        }

    }

}
