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
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
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

import fr.cnes.regards.framework.amqp.Poller;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.domain.CleaningRabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.domain.TestEvent;
import fr.cnes.regards.framework.amqp.utils.IRabbitVirtualHostUtils;
import fr.cnes.regards.framework.amqp.utils.RabbitVirtualHostUtils;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * @author svissier
 *
 */
@ActiveProfiles("rabbit")
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
    private static final String REGARDS_NAMESPACE = "Regards.amqp.";

    /**
     * PROJECT1
     */
    private static final String TENANT = "PROJECT_POLLER_IT";

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
     * bean allowing us to know if the broker is running
     */
    @Autowired
    private IRabbitVirtualHostUtils rabbitVirtualHostUtils;

    /**
     * bean completing AmqpAdmin and RabbitAdmin
     */
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
        rabbitVirtualHostUtils.addVhost(TENANT);
    }

    /**
     * test the polling ONE_TO_MANY to message broker
     */
    @Requirement("REGARDS_DSL_CMP_ARC_170")
    @Purpose("test the polling ONE_TO_MANY to message broker")
    @Test
    public void testPollOneToManyExternal() {
        final Exchange exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY,
                                                                   TENANT, AmqpCommunicationTarget.EXTERNAL);
        final Queue queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT,
                                                          AmqpCommunicationTarget.EXTERNAL);
        regardsAmqpAdmin.declareBinding(queue, exchange, AmqpCommunicationMode.ONE_TO_MANY, TENANT);

        final TestEvent toSend = new TestEvent("test3");
        final TenantWrapper<TestEvent> sended = new TenantWrapper<TestEvent>(toSend, TENANT);
        LOGGER.info("SENDED " + sended);
        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), REGARDS_NAMESPACE + TENANT);
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
        try {
            cleanRabbit(RabbitVirtualHostUtils.getVhostName(TENANT));
        } catch (CleaningRabbitMQVhostException e) {
            LOGGER.debug("Issue during cleaning the broker", e);
        }
    }

    /**
     * test the polling ONE_TO_ONE to message broker
     */
    @Requirement("REGARDS_DSL_CMP_ARC_170")
    @Purpose("test the polling ONE_TO_ONE to message broker")
    @Test
    public void testPollOneToOneExternal() {
        final Exchange exchangeOneToOne = regardsAmqpAdmin.declareExchange(TestEvent.class,
                                                                           AmqpCommunicationMode.ONE_TO_ONE, TENANT,
                                                                           AmqpCommunicationTarget.EXTERNAL);
        final Queue queueOneToOne = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE,
                                                                  TENANT, AmqpCommunicationTarget.EXTERNAL);
        regardsAmqpAdmin.declareBinding(queueOneToOne, exchangeOneToOne, AmqpCommunicationMode.ONE_TO_ONE, TENANT);

        final TestEvent toSend = new TestEvent("test4");
        final TenantWrapper<TestEvent> sended = new TenantWrapper<TestEvent>(toSend, TENANT);
        LOGGER.info("SENDED :" + sended);
        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), RabbitVirtualHostUtils.getVhostName(TENANT));
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
        try {
            cleanRabbit(RabbitVirtualHostUtils.getVhostName(TENANT));
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
