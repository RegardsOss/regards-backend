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
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.amqp.configuration.AmqpConfiguration;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.AddingRabbitMQVhostException;
import fr.cnes.regards.framework.amqp.exception.AddingRabbitMQVhostPermissionException;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.domain.TestEvent;
import fr.cnes.regards.framework.amqp.test.domain.TestReceiver;
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

    private static final String INVALID_JWT = "Invalid JWT";

    @Autowired
    private Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    @Autowired
    private AmqpConfiguration amqpConfiguration;

    /**
     * receiver
     */
    private final TestReceiver testReceiver = new TestReceiver();

    /**
     * event we should receive from the message broker
     */
    private TestEvent received;

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
    private RabbitAdmin rabbitAdmin;

    /**
     * bean provided
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * create and start a message listener to receive the published event
     *
     * @throws RabbitMQVhostException
     * @throws AddingRabbitMQVhostPermissionException
     *             exception that will be thrown if the permission on a Vhost are not added correctly
     * @throws AddingRabbitMQVhostException
     *             exception that will be thrown if the Vhost is not added correctly
     *
     */
    @Before
    public void init() throws RabbitMQVhostException {
        Assume.assumeTrue(amqpConfiguration.brokerRunning());
        final CachingConnectionFactory connectionFactory = amqpConfiguration.createConnectionFactory(TENANT);
        amqpConfiguration.addVhost(TENANT, connectionFactory);
        final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

        final Exchange exchange = amqpConfiguration.declareExchange(TestEvent.class.getName(),
                                                                    AmqpCommunicationMode.ONE_TO_MANY, TENANT);
        final Queue queue = amqpConfiguration.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT);
        amqpConfiguration.declareBinding(queue, exchange, TestEvent.class.getName(), AmqpCommunicationMode.ONE_TO_MANY,
                                         TENANT);

        container.setConnectionFactory(connectionFactory);
        final MessageListenerAdapter messageListener = new MessageListenerAdapter(testReceiver, "handle");
        messageListener.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(messageListener);
        container.addQueues(queue);

        container.start();

    }

    /**
     * Purpose: Send a message to the message broker using the publish client
     */
    @Purpose("Send a message to the message broker using the publish client")
    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Test
    public void testPublishOneToMany() {
        try {
            jwtService.injectToken(TENANT, "USER");
            final TestEvent sended = new TestEvent("test1");

            publisher.publish(sended, AmqpCommunicationMode.ONE_TO_MANY);
            LOGGER.info("SENDED " + sended);
            final int timeToWaitForRabbitToSendUsTheMessage = 2000;

            Thread.sleep(timeToWaitForRabbitToSendUsTheMessage);
            received = testReceiver.getMessage();
            Assert.assertEquals(sended, received);
        } catch (InterruptedException | RabbitMQVhostException e) {
            final String msg = "Publish Test Failed";
            LOGGER.error(msg, e);
            Assert.fail(msg);
        } catch (JwtException e) {
            LOGGER.error(INVALID_JWT);
            Assert.fail(INVALID_JWT);
        }
    }

    /**
     * Purpose: Send a message to the message broker using the publish client with priority
     */
    @Purpose("Send a message to the message broker using the publish client with priority")
    @Test
    public void testPublishPriority() {
        try {
            jwtService.injectToken(TENANT, "USER");
            final TestEvent priority0 = new TestEvent("priority 0");
            final TestEvent priority1 = new TestEvent("priority 1");
            final TestEvent priority02 = new TestEvent("priority 02");
            final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

            final Exchange exchange = amqpConfiguration.declareExchange(TestEvent.class.getName(),
                                                                        AmqpCommunicationMode.ONE_TO_ONE, TENANT);
            final Queue queue = amqpConfiguration.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE,
                                                               TENANT);
            amqpConfiguration.declareBinding(queue, exchange, TestEvent.class.getName(),
                                             AmqpCommunicationMode.ONE_TO_ONE, TENANT);
            publisher.publish(priority0, AmqpCommunicationMode.ONE_TO_ONE, 0);
            publisher.publish(priority1, AmqpCommunicationMode.ONE_TO_ONE, 1);
            publisher.publish(priority02, AmqpCommunicationMode.ONE_TO_ONE, 0);

            SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), TENANT);
            TenantWrapper<TestEvent> wrappedMessage = (TenantWrapper<TestEvent>) rabbitTemplate
                    .receiveAndConvert(TestEvent.class.getName());
            SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
            Assert.assertEquals(priority1, wrappedMessage.getContent());

            SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), TENANT);
            wrappedMessage = (TenantWrapper<TestEvent>) rabbitTemplate.receiveAndConvert(TestEvent.class.getName());
            SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
            Assert.assertEquals(priority0, wrappedMessage.getContent());

            SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), TENANT);
            wrappedMessage = (TenantWrapper<TestEvent>) rabbitTemplate.receiveAndConvert(TestEvent.class.getName());
            SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
            Assert.assertEquals(priority02, wrappedMessage.getContent());
        } catch (RabbitMQVhostException e) {
            final String msg = "Publish Test Failed";
            LOGGER.error(msg, e);
        } catch (JwtException e) {
            LOGGER.error(INVALID_JWT);
            Assert.fail(INVALID_JWT);
        }
    }
}
