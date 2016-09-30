/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;
/*
 * LICENSE_PLACEHOLDER
 */

import static org.junit.Assert.assertEquals;

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

import fr.cnes.regards.modules.core.amqp.domain.TestEvent;
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
@SpringBootTest
@DirtiesContext
public class PublisherIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublisherIT.class);

    private TestEvent received_;

    private static final String testEventName = "fr.cnes.regards.modules.core.amqp.domain.TestEvent";

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = testEventName, durable = "true"), exchange = @Exchange(value = "PROJECT", type = "direct", durable = "true"), key = testEventName))
    public final void handle(TestEvent pMessage) {
        LOGGER.info("================ Received " + pMessage);
        received_ = pMessage;
    }

    @Autowired
    private JWTService jwtService_;

    @Autowired
    private Publisher publisher_;

    @Before
    public void init() {
        String jwt = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
        try {
            SecurityContextHolder.getContext().setAuthentication(jwtService_.parseToken(new JWTAuthentication(jwt)));
        }
        catch (InvalidJwtException | MissingClaimException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void publishTest() {
        try {
            TestEvent sended = new TestEvent("test1");

            publisher_.publish(sended);
            LOGGER.info("SENDED " + sended);
            Thread.sleep(2000);
            assertEquals(sended, received_);
        }
        catch (Exception e) {
            LOGGER.error("publish Test Failed", e);
            Assert.fail("publish Test Failed");
        }
    }
}
