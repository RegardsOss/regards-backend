/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.modules.core.amqp.domain.TestEvent;
import fr.cnes.regards.modules.core.amqp.domain.TestReceiver;
import fr.cnes.regards.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.security.utils.jwt.JWTService;
import fr.cnes.regards.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.security.utils.jwt.exception.MissingClaimException;

/*
 * LICENSE_PLACEHOLDER
 */

/**
 * @author svissier
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { SubscriberTestsConfiguration.class })
@SpringBootTest
@DirtiesContext
public class SubscriberIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberIT.class);

    @Autowired
    private Subscriber subscriber_;

    @Autowired
    private ConnectionFactory connectionFactory_;

    private TestEvent received_;

    private TestReceiver receiver_;

    private SimpleMessageListenerContainer container_;

    @Autowired
    private JWTService jwtService_;

    @Autowired
    private RabbitTemplate rabbitTemplate_;

    @Autowired
    private Jackson2JsonMessageConverter jackson2JsonMessageConverter_;

    @Before
    public void init() {
        String jwt = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
        try {
            SecurityContextHolder.getContext().setAuthentication(jwtService_.parseToken(new JWTAuthentication(jwt)));
            receiver_ = new TestReceiver();
            container_ = subscriber_.subscribeTo(TestEvent.class, receiver_, connectionFactory_);
            container_.start();
        }
        catch (InvalidJwtException | MissingClaimException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (Exception e) {
            LOGGER.error("EXCEPTION ", e);
            Assert.fail("Exception");
        }
    }

    @Test
    public void testSubscribeTo() {
        TestEvent sended = new TestEvent("test2");
        LOGGER.info("SENDED " + sended);
        rabbitTemplate_.convertAndSend("PROJECT", TestEvent.class.getName(), sended);

        try {
            Thread.sleep(2000);
        }
        catch (InterruptedException e) {
            LOGGER.error("Sleep Failed", e);
            Assert.fail("Sleep Failed");
        }
        LOGGER.info("=================RECEIVED " + receiver_.getMessage());
        received_ = receiver_.getMessage();
        assertEquals(sended, received_);

    }

    @After
    public void destroy() {
        container_.stop();
    }

}
