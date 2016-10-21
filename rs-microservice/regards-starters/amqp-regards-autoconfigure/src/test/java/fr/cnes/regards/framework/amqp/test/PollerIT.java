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
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.amqp.Poller;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.domain.TestEvent;
import fr.cnes.regards.framework.amqp.utils.RabbitVirtualHostUtils;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * @author svissier
 *
 */
@Profile("rabbit")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AmqpTestsConfiguration.class })
@SpringBootTest(classes = Application.class)
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
     * bean used to send message to the broker
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * bean to test
     */
    @Autowired
    private Poller poller;

    /**
     * configuration bean
     */
    @Autowired
    private RabbitVirtualHostUtils rabbitVirtualHostUtils;

    @Autowired
    private RegardsAmqpAdmin regardsAmqpAdmin;

    /**
     *
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    @Before
    public void init() throws RabbitMQVhostException {
        Assume.assumeTrue(rabbitVirtualHostUtils.brokerRunning());
        final CachingConnectionFactory connectionFactory = regardsAmqpAdmin.createConnectionFactory(TENANT);
        rabbitVirtualHostUtils.addVhost(TENANT, connectionFactory);
    }

    /**
     * test the polling ONE_TO_MANY to message broker
     */
    @Requirement("REGARDS_DSL_CMP_ARC_170")
    @Purpose("test the polling ONE_TO_MANY to message broker")
    @Test
    public void testPollOneToManyExternal() {
        final Exchange exchange = regardsAmqpAdmin.declareExchange(TestEvent.class.getName(),
                                                                   AmqpCommunicationMode.ONE_TO_MANY, TENANT,
                                                                   AmqpCommunicationTarget.EXTERNAL);
        final Queue queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT,
                                                          AmqpCommunicationTarget.EXTERNAL);
        regardsAmqpAdmin.declareBinding(queue, exchange, AmqpCommunicationMode.ONE_TO_MANY, TENANT);

        final TestEvent toSend = new TestEvent("test3");
        final TenantWrapper<TestEvent> sended = new TenantWrapper<TestEvent>(toSend, TENANT);
        LOGGER.info("SENDED " + sended);
        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), TENANT);
        rabbitTemplate.convertAndSend(TestEvent.class.getName(), "", sended);
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());

        final TenantWrapper<TestEvent> wrapperReceived;
        try {
            wrapperReceived = poller.poll(TENANT, TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY,
                                          AmqpCommunicationTarget.EXTERNAL);
            LOGGER.info("=================RECEIVED " + wrapperReceived.getContent());
            final TestEvent received = wrapperReceived.getContent();
            Assert.assertEquals(toSend, received);
        } catch (RabbitMQVhostException e) {
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
    public void testPollOneToOneExternal() {
        final Exchange exchangeOneToOne = regardsAmqpAdmin.declareExchange(TestEvent.class.getName(),
                                                                           AmqpCommunicationMode.ONE_TO_ONE, TENANT,
                                                                           AmqpCommunicationTarget.EXTERNAL);
        final Queue queueOneToOne = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE,
                                                                  TENANT, AmqpCommunicationTarget.EXTERNAL);
        regardsAmqpAdmin.declareBinding(queueOneToOne, exchangeOneToOne, AmqpCommunicationMode.ONE_TO_ONE, TENANT);

        final TestEvent toSend = new TestEvent("test4");
        final TenantWrapper<TestEvent> sended = new TenantWrapper<TestEvent>(toSend, TENANT);
        LOGGER.info("SENDED :" + sended);
        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), TENANT);
        // for one to one communication, exchange is named after the application and not what we are exchanging
        rabbitTemplate.convertAndSend("REGARDS", TestEvent.class.getName(), sended);
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());

        final TenantWrapper<TestEvent> wrapperReceived;
        try {
            wrapperReceived = poller.poll(TENANT, TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE,
                                          AmqpCommunicationTarget.EXTERNAL);
            LOGGER.info("=================RECEIVED :" + wrapperReceived.getContent());
            final TestEvent received = wrapperReceived.getContent();
            Assert.assertEquals(toSend, received);
        } catch (RabbitMQVhostException e) {
            final String msg = "Polling one to one Test Failed";
            LOGGER.error(msg, e);
            Assert.fail(msg);
        }

    }

}
