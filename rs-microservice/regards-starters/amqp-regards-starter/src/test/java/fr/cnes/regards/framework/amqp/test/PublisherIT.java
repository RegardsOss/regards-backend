/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test;
/*
 * LICENSE_PLACEHOLDER
 */

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
import org.springframework.beans.factory.annotation.Value;
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

import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.MultitenantAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.RabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.domain.CleaningRabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.domain.TestEvent;
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
public class PublisherIT {

    /**
     * LOGGER used to populate logs when needed
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PublisherIT.class);

    /**
     * default message error
     */
    private static final String PUBLISH_TEST_FAILED = "Publish Test Failed";

    /**
     * \/
     */
    private static final String SLASH = "/";

    /**
     * Static default tenant
     */
    @Value("${regards.tenant}")
    private String tenant;

    /**
     * bean used to clean the broker after tests
     */
    @Autowired
    private RestTemplate restTemplate;

    /**
     * configuration
     */
    @Autowired
    private MultitenantAmqpAdmin amqpConfiguration;

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
    private IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    /**
     * create and start a message listener to receive the published event
     *
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    @Before
    public void init() throws RabbitMQVhostException {
        Assume.assumeTrue(rabbitVirtualHostAdmin.brokerRunning());
        rabbitVirtualHostAdmin.addVhost(tenant);
    }

    /**
     * Purpose: Send a message to the message broker using the publish client
     */
    @Purpose("Send a message to the message broker using the publish client")
    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Test
    public void testPublishOneToManyExternal() {
        try {
            final Exchange exchange = amqpConfiguration.declareExchange(tenant, TestEvent.class, WorkerMode.ALL,
                                                                        Target.ALL);
            final Queue queue = amqpConfiguration.declareQueue(tenant, TestEvent.class, WorkerMode.ALL, Target.ALL);
            amqpConfiguration.declareBinding(tenant, queue, exchange, WorkerMode.ALL);

            final TestEvent sended = new TestEvent("test1");

            publisher.publish(sended, WorkerMode.ALL, Target.ALL, 0);
            LOGGER.info("SENDED " + sended);

            SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(),
                                      RabbitVirtualHostAdmin.getVhostName(tenant));
            // CHECKSTYLE:OFF
            @SuppressWarnings("unchecked")
            final TenantWrapper<TestEvent> wrappedMessage = (TenantWrapper<TestEvent>) rabbitTemplate
                    .receiveAndConvert(amqpConfiguration.getQueueName(TestEvent.class, WorkerMode.ALL, Target.ALL));
            // CHECKSTYLE:ON
            SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());

            final TestEvent received = wrappedMessage.getContent();
            Assert.assertEquals(sended, received);
        } catch (RabbitMQVhostException e) {
            LOGGER.error(PUBLISH_TEST_FAILED, e);
            Assert.fail(PUBLISH_TEST_FAILED);
        }
        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(tenant));
        } catch (CleaningRabbitMQVhostException e) {
            LOGGER.debug("Issue during cleaning the broker", e);
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
            final Exchange exchange = amqpConfiguration.declareExchange(tenant, TestEvent.class, WorkerMode.SINGLE,
                                                                        Target.ALL);
            final Queue queue = amqpConfiguration.declareQueue(tenant, TestEvent.class, WorkerMode.SINGLE, Target.ALL);
            amqpConfiguration.declareBinding(tenant, queue, exchange, WorkerMode.SINGLE);

            final TestEvent priority0 = new TestEvent("priority 0");
            final TestEvent priority1 = new TestEvent("priority 1");
            final TestEvent priority02 = new TestEvent("priority 02");

            publisher.publish(priority0, WorkerMode.SINGLE, Target.ALL, 0);
            publisher.publish(priority1, WorkerMode.SINGLE, Target.ALL, 1);
            publisher.publish(priority02, WorkerMode.SINGLE, Target.ALL, 0);

            SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(),
                                      RabbitVirtualHostAdmin.getVhostName(tenant));
            TenantWrapper<TestEvent> wrappedMessage = (TenantWrapper<TestEvent>) rabbitTemplate
                    .receiveAndConvert(amqpConfiguration.getQueueName(TestEvent.class, WorkerMode.SINGLE, Target.ALL));
            SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
            Assert.assertEquals(priority1, wrappedMessage.getContent());

            SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(),
                                      RabbitVirtualHostAdmin.getVhostName(tenant));
            wrappedMessage = (TenantWrapper<TestEvent>) rabbitTemplate
                    .receiveAndConvert(amqpConfiguration.getQueueName(TestEvent.class, WorkerMode.SINGLE, Target.ALL));
            SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
            Assert.assertEquals(priority0, wrappedMessage.getContent());

            SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(),
                                      RabbitVirtualHostAdmin.getVhostName(tenant));
            wrappedMessage = (TenantWrapper<TestEvent>) rabbitTemplate
                    .receiveAndConvert(amqpConfiguration.getQueueName(TestEvent.class, WorkerMode.SINGLE, Target.ALL));
            SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
            Assert.assertEquals(priority02, wrappedMessage.getContent());
        } catch (RabbitMQVhostException e) {
            LOGGER.error(PUBLISH_TEST_FAILED, e);
            Assert.fail(PUBLISH_TEST_FAILED);
        }
        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(tenant));
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
        final List<String> existingVhost = rabbitVirtualHostAdmin.retrieveVhostList();
        if (existingVhost.stream().filter(vhost -> vhost.equals(pTenant1)).findAny().isPresent()) {
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.AUTHORIZATION, rabbitVirtualHostAdmin.setBasic());
            final HttpEntity<Void> request = new HttpEntity<>(headers);
            final ResponseEntity<String> response = restTemplate
                    .exchange(rabbitVirtualHostAdmin.getRabbitApiVhostEndpoint() + SLASH + pTenant1, HttpMethod.DELETE,
                              request, String.class);
            final int statusValue = response.getStatusCodeValue();
            // if successful or 404 then the broker is clean
            if (!(rabbitVirtualHostAdmin.isSuccess(statusValue) || (statusValue == HttpStatus.NOT_FOUND.value()))) {
                throw new CleaningRabbitMQVhostException(response.getBody());
            }
        }
    }
}
