/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test;
/*
 * LICENSE_PLACEHOLDER
 */

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.domain.TestEvent;
import fr.cnes.regards.framework.amqp.utils.IRabbitVirtualHostUtils;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * @author svissier
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AmqpTestsConfiguration.class })
@SpringBootTest(classes = Application.class)
@DirtiesContext
public class PublisherIT {

    /**
     * LOGGER used to populate logs when needed
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PublisherIT.class);

    /**
     * fake tenant
     */
    private static final String TENANT = "PROJECT";

    /**
     * Invalid JWT
     */
    private static final String INVALID_JWT = "Invalid JWT";

    /**
     * Role used into tests for the jwt
     */
    private static final String ROLE = "USER";

    /**
     * default message error
     */
    private static final String PUBLISH_TEST_FAILED = "Publish Test Failed";

    /**
     * configuration
     */
    @Autowired
    private RegardsAmqpAdmin amqpConfiguration;

    /**
     * bean used to generate and place JWT into the context
     */
    @Autowired
    private JWTService jwtService;

    /**
     * bean used to publish message to the message broker and which is tested here
     */
    @Autowired
    private Publisher publisher;

    /**
     * bean provided
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * bean allowing us to know if the broker is running
     */
    @Autowired
    private IRabbitVirtualHostUtils rabbitVirtualHostUtils;

    /**
     * create and start a message listener to receive the published event
     *
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    @Before
    public void init() throws RabbitMQVhostException {
        Assume.assumeTrue(rabbitVirtualHostUtils.brokerRunning());
        final CachingConnectionFactory connectionFactory = amqpConfiguration.createConnectionFactory(TENANT);
        rabbitVirtualHostUtils.addVhost(TENANT, connectionFactory);
    }

    /**
     * Purpose: Send a message to the message broker using the publish client
     */
    @Purpose("Send a message to the message broker using the publish client")
    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Test
    public void testPublishOneToManyExternal() {
        try {
            jwtService.injectToken(TENANT, ROLE);

            final Exchange exchange = amqpConfiguration.declareExchange(TestEvent.class,
                                                                        AmqpCommunicationMode.ONE_TO_MANY, TENANT,
                                                                        AmqpCommunicationTarget.EXTERNAL);
            final Queue queue = amqpConfiguration.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY,
                                                               TENANT, AmqpCommunicationTarget.EXTERNAL);
            amqpConfiguration.declareBinding(queue, exchange, AmqpCommunicationMode.ONE_TO_MANY, TENANT);

            final TestEvent sended = new TestEvent("test1");

            publisher.publish(sended, AmqpCommunicationMode.ONE_TO_MANY, AmqpCommunicationTarget.EXTERNAL);
            LOGGER.info("SENDED " + sended);

            SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), TENANT);
            // CHECKSTYLE:OFF
            @SuppressWarnings("unchecked")
            final TenantWrapper<TestEvent> wrappedMessage = (TenantWrapper<TestEvent>) rabbitTemplate
                    .receiveAndConvert(amqpConfiguration.getQueueName(TestEvent.class,
                                                                      AmqpCommunicationMode.ONE_TO_MANY, AmqpCommunicationTarget.EXTERNAL));
            // CHECKSTYLE:ON
            SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());

            final TestEvent received = wrappedMessage.getContent();
            Assert.assertEquals(sended, received);
        } catch (RabbitMQVhostException e) {
            LOGGER.error(PUBLISH_TEST_FAILED, e);
            Assert.fail(PUBLISH_TEST_FAILED);
        } catch (JwtException e) {
            LOGGER.error(INVALID_JWT);
            Assert.fail(INVALID_JWT);
        }
    }

    /**
     * Purpose: Send a message to the message broker using the publish client with priority
     */
    @SuppressWarnings("unchecked")
    @Purpose("Send a message to the message broker using the publish client with priority")
    @Test
    public void testPublishPriorityExternal() {
        try {
            jwtService.injectToken(TENANT, ROLE);

            final Exchange exchange = amqpConfiguration.declareExchange(TestEvent.class,
                                                                        AmqpCommunicationMode.ONE_TO_ONE, TENANT,
                                                                        AmqpCommunicationTarget.EXTERNAL);
            final Queue queue = amqpConfiguration.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE,
                                                               TENANT, AmqpCommunicationTarget.EXTERNAL);
            amqpConfiguration.declareBinding(queue, exchange, AmqpCommunicationMode.ONE_TO_ONE, TENANT);

            final TestEvent priority0 = new TestEvent("priority 0");
            final TestEvent priority1 = new TestEvent("priority 1");
            final TestEvent priority02 = new TestEvent("priority 02");

            publisher.publish(priority0, AmqpCommunicationMode.ONE_TO_ONE, AmqpCommunicationTarget.EXTERNAL, 0);
            publisher.publish(priority1, AmqpCommunicationMode.ONE_TO_ONE, AmqpCommunicationTarget.EXTERNAL, 1);
            publisher.publish(priority02, AmqpCommunicationMode.ONE_TO_ONE, AmqpCommunicationTarget.EXTERNAL, 0);

            SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), TENANT);
            TenantWrapper<TestEvent> wrappedMessage = (TenantWrapper<TestEvent>) rabbitTemplate
                    .receiveAndConvert(amqpConfiguration.getQueueName(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE,
                                                                      AmqpCommunicationTarget.EXTERNAL));
            SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
            Assert.assertEquals(priority1, wrappedMessage.getContent());

            SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), TENANT);
            wrappedMessage = (TenantWrapper<TestEvent>) rabbitTemplate.receiveAndConvert(amqpConfiguration
                    .getQueueName(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE, AmqpCommunicationTarget.EXTERNAL));
            SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
            Assert.assertEquals(priority0, wrappedMessage.getContent());

            SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), TENANT);
            wrappedMessage = (TenantWrapper<TestEvent>) rabbitTemplate.receiveAndConvert(amqpConfiguration
                    .getQueueName(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE, AmqpCommunicationTarget.EXTERNAL));
            SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
            Assert.assertEquals(priority02, wrappedMessage.getContent());
        } catch (RabbitMQVhostException e) {
            LOGGER.error(PUBLISH_TEST_FAILED, e);
            Assert.fail(PUBLISH_TEST_FAILED);
        } catch (JwtException e) {
            LOGGER.error(INVALID_JWT);
            Assert.fail(INVALID_JWT);
        }
    }
}
