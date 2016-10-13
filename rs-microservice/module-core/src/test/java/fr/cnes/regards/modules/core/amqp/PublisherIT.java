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
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;
import fr.cnes.regards.modules.core.amqp.domain.TestEvent;
import fr.cnes.regards.modules.core.amqp.utils.TenantWrapper;
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
    private static final String TEST_EEVENT_NAME = "fr.cnes.regards.modules.core.amqp.domain.TestEvent";

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

    // CHECKSTYLE:OFF
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = TEST_EEVENT_NAME, durable = "true"),
            exchange = @Exchange(value = "REGARDS", type = "direct", durable = "true"), key = TEST_EEVENT_NAME))
    // CHECKSTYLE:ON
    public final void handle(TenantWrapper<TestEvent> pMessage) {
        LOGGER.info("================ Received " + pMessage.getTenant() + " : " + pMessage.getContent());
        received = pMessage.getContent();
    }

    /**
     * set a tenant into the security context, where it will be taken from to publish in name of the right tenant
     */
    @Before
    public void init() {
        final String jwt = jwtService.generateToken("PROJECT", "email", "SVG", "USER");
        try {
            SecurityContextHolder.getContext().setAuthentication(jwtService.parseToken(new JWTAuthentication(jwt)));
        } catch (InvalidJwtException | MissingClaimException e) {
            e.printStackTrace();
        }
    }

    /**
     * Purpose: Send a message to the message broker using the publish client
     */
    @Purpose("Send a message to the message broker using the publish client")
    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Test
    public void publishTest() {

        try {
            final TestEvent sended = new TestEvent("test1");

            publisher.publish(sended);
            LOGGER.info("SENDED " + sended);
            final int timeToWaitForRabbitToSendUsTheMessage = 2000;

            Thread.sleep(timeToWaitForRabbitToSendUsTheMessage);
            Assert.assertEquals(sended, received);
        } catch (InterruptedException | AddingRabbitMQVhostException | AddingRabbitMQVhostPermissionException e) {
            final String msg = "Publish Test Failed";
            LOGGER.error(msg, e);
            Assert.fail(msg);
        }

    }
}
