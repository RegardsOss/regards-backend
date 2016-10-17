/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;
/*
 * LICENSE_PLACEHOLDER
 */

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;
import fr.cnes.regards.modules.core.amqp.configuration.AmqpConfiguration;
import fr.cnes.regards.modules.core.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.modules.core.amqp.domain.TestEvent;
import fr.cnes.regards.modules.core.amqp.domain.TestReceiver;
import fr.cnes.regards.modules.core.exception.AddingRabbitMQVhostException;
import fr.cnes.regards.modules.core.exception.AddingRabbitMQVhostPermissionException;
import fr.cnes.regards.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.security.utils.jwt.JWTService;
import fr.cnes.regards.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.security.utils.jwt.exception.MissingClaimException;

/**
 * @author svissier
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AmqpTestsConfiguration.class })
@SpringBootTest(classes = ApplicationTest.class)
@DirtiesContext
public class PublisherIT {

    /**
     * LOGGER used to populate logs when needed
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PublisherIT.class);

    /**
     * name of the queue in which the message should go and from which we will receive the event
     */
    private static final String TEST_EVENT_NAME = "fr.cnes.regards.modules.core.amqp.domain.TestEvent";

    /**
     * fake tenant
     */
    private static final String TENANT = "PROJECT";

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

    @Autowired
    private RabbitAdmin rabbitAdmin;

    /**
     * create and start a message listener to receive the published event
     *
     * @throws AddingRabbitMQVhostPermissionException
     *             exception that will be thrown if the permission on a Vhost are not added correctly
     * @throws AddingRabbitMQVhostException
     *             exception that will be thrown if the Vhost is not added correctly
     *
     */
    @Before
    public void init() throws AddingRabbitMQVhostException, AddingRabbitMQVhostPermissionException {
        amqpConfiguration.addVhost(TENANT);
        ((CachingConnectionFactory) rabbitAdmin.getRabbitTemplate().getConnectionFactory()).setVirtualHost(TENANT);
        final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        final FanoutExchange exchange = new FanoutExchange(TEST_EVENT_NAME);
        rabbitAdmin.declareExchange(exchange);
        final Queue queue = new Queue(amqpConfiguration.getUniqueName(), true);
        rabbitAdmin.declareQueue(queue);
        final Binding binding = BindingBuilder.bind(queue).to(exchange);
        rabbitAdmin.declareBinding(binding);

        container.setConnectionFactory(rabbitAdmin.getRabbitTemplate().getConnectionFactory());
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
        final String jwt = jwtService.generateToken(TENANT, "email", "SVG", "USER");
        try {
            SecurityContextHolder.getContext().setAuthentication(jwtService.parseToken(new JWTAuthentication(jwt)));
            final TestEvent sended = new TestEvent("test1");

            publisher.publish(sended, AmqpCommunicationMode.ONE_TO_MANY);
            LOGGER.info("SENDED " + sended);
            final int timeToWaitForRabbitToSendUsTheMessage = 2000;

            Thread.sleep(timeToWaitForRabbitToSendUsTheMessage);
            received = testReceiver.getMessage();
            Assert.assertEquals(sended, received);
        } catch (InterruptedException | AddingRabbitMQVhostException | AddingRabbitMQVhostPermissionException e) {
            final String msg = "Publish Test Failed";
            LOGGER.error(msg, e);
            Assert.fail(msg);
        } catch (InvalidJwtException | MissingClaimException e) {
            e.printStackTrace();
        }
    }
}
