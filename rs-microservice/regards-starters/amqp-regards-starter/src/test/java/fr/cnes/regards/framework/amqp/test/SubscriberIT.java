/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test;

import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import fr.cnes.regards.framework.amqp.Subscriber;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.domain.CleaningRabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.domain.TestEvent;
import fr.cnes.regards.framework.amqp.test.domain.TestReceiver;
import fr.cnes.regards.framework.amqp.utils.IRabbitVirtualHostUtils;
import fr.cnes.regards.framework.amqp.utils.RabbitVirtualHostUtils;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/*
 * LICENSE_PLACEHOLDER
 */

/**
 * @author svissier
 *
 */
@ActiveProfiles("rabbit")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AmqpTestsConfiguration.class })
@SpringBootTest(classes = Application.class)
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
     * =================RECEIVED
     */
    private static final String RECEIVED = "=================RECEIVED ";

    /**
     * SENDED
     */
    private static final String SENDED = "SENDED ";

    /**
     * \/
     */
    private static final String SLASH = "/";

    /**
     * bean used to clean the broker after tests
     */
    @Autowired
    private RestTemplate restTemplate;

    /**
     * bean to test
     */
    @Autowired
    private Subscriber subscriberOneToManyExternal;

    /**
     * message receiver
     */
    private TestReceiver receiverOneToMany;

    /**
     * Template to send original message
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * bean used to know if the broker is running
     */
    @Autowired
    private RegardsAmqpAdmin amqpConfiguration;

    /**
     * bean allowing us to know if the broker is running
     */
    @Autowired
    private IRabbitVirtualHostUtils rabbitVirtualHostUtils;

    /**
     * initialization ran before each test case
     */
    @Before
    public void init() {
        Assume.assumeTrue(rabbitVirtualHostUtils.brokerRunning());
        receiverOneToMany = new TestReceiver();

        try {
            subscriberOneToManyExternal.subscribeTo(TestEvent.class, receiverOneToMany,
                                                    AmqpCommunicationMode.ONE_TO_MANY,
                                                    AmqpCommunicationTarget.ALL);

        } catch (RabbitMQVhostException e) {
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
    public void testSubscribeToOneToManyExternal() {
        final TestEvent toSend = new TestEvent("test one to many");
        final TenantWrapper<TestEvent> sended = new TenantWrapper<TestEvent>(toSend, TENANT);
        LOGGER.info(SENDED + sended);
        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), RabbitVirtualHostUtils.getVhostName(TENANT));
        // CHECKSTYLE:OFF
        rabbitTemplate.convertAndSend(
                                      amqpConfiguration.getExchangeName(TestEvent.class.getName(),
                                                                        AmqpCommunicationTarget.ALL),
                                      amqpConfiguration.getRoutingKey("", AmqpCommunicationMode.ONE_TO_MANY), sended,
                                      pMessage -> {
                                          final MessageProperties propertiesWithPriority = pMessage
                                                  .getMessageProperties();
                                          propertiesWithPriority.setPriority(0);
                                          return new Message(pMessage.getBody(), propertiesWithPriority);
                                      });
        // CHECKSTYLE:ON
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());

        try {
            Thread.sleep(SLEEP_TIME); // NOSONAR : passive test so we have to wait for the message to be sent
        } catch (InterruptedException e) {
            LOGGER.error(SLEEP_FAIL, e);
            Assert.fail(SLEEP_FAIL);
        }
        LOGGER.info(RECEIVED + receiverOneToMany.getMessage());
        Assert.assertEquals(toSend, receiverOneToMany.getMessage());
        try {
            cleanRabbit(TENANT);
        } catch (CleaningRabbitMQVhostException e) {
            LOGGER.debug("Issue during cleaning the broker", e);
        }
    }

    /**
     * delete the virtual host if existing
     *
     * @param pTenant1
     *            tenant to clean
     * @throws CleaningRabbitMQVhostException
     *             any issues that could occur
     */
    private void cleanRabbit(String pTenant1) throws CleaningRabbitMQVhostException {
        final List<String> existingVhost = rabbitVirtualHostUtils.retrieveVhostList();
        if (existingVhost.stream().filter(vhost -> vhost.equals(pTenant1)).findAny().isPresent()) {
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.AUTHORIZATION, rabbitVirtualHostUtils.setBasic());
            final HttpEntity<Void> request = new HttpEntity<>(headers);
            final ResponseEntity<String> response = restTemplate
                    .exchange(rabbitVirtualHostUtils.getRabbitApiVhostEndpoint() + SLASH + pTenant1, HttpMethod.DELETE,
                              request, String.class);
            final int statusValue = response.getStatusCodeValue();
            // if successful or 404 then the broker is clean
            if (!(rabbitVirtualHostUtils.isSuccess(statusValue) || (statusValue == HttpStatus.NOT_FOUND.value()))) {
                throw new CleaningRabbitMQVhostException(response.getBody());
            }
        }
    }

}
