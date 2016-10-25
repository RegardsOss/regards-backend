/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test;

import java.util.List;

import org.junit.After;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.domain.CleaningRabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.domain.GettingRabbitMQBindingException;
import fr.cnes.regards.framework.amqp.test.domain.GettingRabbitMQExchangeException;
import fr.cnes.regards.framework.amqp.test.domain.GettingRabbitMQQueueException;
import fr.cnes.regards.framework.amqp.test.domain.RestBinding;
import fr.cnes.regards.framework.amqp.test.domain.RestExchange;
import fr.cnes.regards.framework.amqp.test.domain.RestQueue;
import fr.cnes.regards.framework.amqp.test.domain.TestEvent;
import fr.cnes.regards.framework.amqp.utils.IRabbitVirtualHostUtils;

/**
 * @author svissier
 *
 */
@ActiveProfiles("rabbit")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class RegardsAmqpAdminIT {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RegardsAmqpAdminIT.class);

    /**
     * Tenant_Test_1
     */
    private static final String TENANT1 = "Tenant_Test_1";

    /**
     * Tenant_Test_2
     */
    private static final String TENANT2 = "Tenant_Test_2";

    /**
     * \/
     */
    private static final String SLASH = "/";

    /**
     * bean tested
     */
    @Autowired
    private RegardsAmqpAdmin regardsAmqpAdmin;

    /**
     * bean helping us to test
     */
    @Autowired
    private IRabbitVirtualHostUtils rabbitVirtualHostUtils;

    /**
     * bean allowing us to clena the broker
     */
    @Autowired
    private RestTemplate restTemplate;

    @Before
    public void init() {
        Assume.assumeTrue(rabbitVirtualHostUtils.brokerRunning());
    }

    @Test
    public void testDeclareBindingOneToOneInternal() {
        try {
            cleanRabbit(TENANT1);
            CachingConnectionFactory cf = regardsAmqpAdmin.createConnectionFactory(TENANT1);
            rabbitVirtualHostUtils.addVhost(TENANT1, cf);

            Exchange exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE,
                                                                 TENANT1, AmqpCommunicationTarget.INTERNAL);
            Queue queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE, TENANT1,
                                                        AmqpCommunicationTarget.INTERNAL);
            regardsAmqpAdmin.declareBinding(queue, exchange, AmqpCommunicationMode.ONE_TO_ONE, TENANT1);
            List<RestBinding> declaredBindings = retrieveBinding(TENANT1);
            RestBinding restBinding = declaredBindings.get(0);
            Assert.assertEquals(2, declaredBindings.size());
            Assert.assertEquals(TENANT1, restBinding.getVhost());

            cleanRabbit(TENANT2);
            cf = regardsAmqpAdmin.createConnectionFactory(TENANT2);
            rabbitVirtualHostUtils.addVhost(TENANT2, cf);

            exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE, TENANT2,
                                                        AmqpCommunicationTarget.INTERNAL);
            queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE, TENANT2,
                                                  AmqpCommunicationTarget.INTERNAL);
            regardsAmqpAdmin.declareBinding(queue, exchange, AmqpCommunicationMode.ONE_TO_ONE, TENANT2);
            declaredBindings = retrieveBinding(TENANT2);
            restBinding = declaredBindings.get(0);
            Assert.assertEquals(2, declaredBindings.size());
            Assert.assertEquals(TENANT2, restBinding.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQBindingException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        }
    }

    @Test
    public void testDeclareBindingOneToManyInternal() {
        try {
            cleanRabbit(TENANT1);
            CachingConnectionFactory cf = regardsAmqpAdmin.createConnectionFactory(TENANT1);
            rabbitVirtualHostUtils.addVhost(TENANT1, cf);

            Exchange exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY,
                                                                 TENANT1, AmqpCommunicationTarget.INTERNAL);
            Queue queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT1,
                                                        AmqpCommunicationTarget.INTERNAL);
            regardsAmqpAdmin.declareBinding(queue, exchange, AmqpCommunicationMode.ONE_TO_MANY, TENANT1);
            List<RestBinding> declaredBindings = retrieveBinding(TENANT1);
            RestBinding restBinding = declaredBindings.get(0);
            Assert.assertEquals(2, declaredBindings.size());
            Assert.assertEquals(TENANT1, restBinding.getVhost());

            cleanRabbit(TENANT2);
            cf = regardsAmqpAdmin.createConnectionFactory(TENANT2);
            rabbitVirtualHostUtils.addVhost(TENANT2, cf);

            exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT2,
                                                        AmqpCommunicationTarget.INTERNAL);
            queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT2,
                                                  AmqpCommunicationTarget.INTERNAL);
            regardsAmqpAdmin.declareBinding(queue, exchange, AmqpCommunicationMode.ONE_TO_MANY, TENANT2);
            declaredBindings = retrieveBinding(TENANT2);
            restBinding = declaredBindings.get(0);
            Assert.assertEquals(2, declaredBindings.size());
            Assert.assertEquals(TENANT2, restBinding.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQBindingException e) {
            Assert.fail("Failed to clean " + TENANT1);
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        }
    }

    @Test
    public void testDeclareBindingOneToOneExternal() {
        try {
            cleanRabbit(TENANT1);
            CachingConnectionFactory cf = regardsAmqpAdmin.createConnectionFactory(TENANT1);
            rabbitVirtualHostUtils.addVhost(TENANT1, cf);

            Exchange exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE,
                                                                 TENANT1, AmqpCommunicationTarget.EXTERNAL);
            Queue queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE, TENANT1,
                                                        AmqpCommunicationTarget.EXTERNAL);
            regardsAmqpAdmin.declareBinding(queue, exchange, AmqpCommunicationMode.ONE_TO_ONE, TENANT1);
            List<RestBinding> declaredBindings = retrieveBinding(TENANT1);
            RestBinding restBinding = declaredBindings.get(0);
            Assert.assertEquals(2, declaredBindings.size());
            Assert.assertEquals(TENANT1, restBinding.getVhost());

            cleanRabbit(TENANT2);
            cf = regardsAmqpAdmin.createConnectionFactory(TENANT2);
            rabbitVirtualHostUtils.addVhost(TENANT2, cf);

            exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE, TENANT2,
                                                        AmqpCommunicationTarget.EXTERNAL);
            queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE, TENANT2,
                                                  AmqpCommunicationTarget.EXTERNAL);
            regardsAmqpAdmin.declareBinding(queue, exchange, AmqpCommunicationMode.ONE_TO_ONE, TENANT2);
            declaredBindings = retrieveBinding(TENANT2);
            restBinding = declaredBindings.get(0);
            Assert.assertEquals(2, declaredBindings.size());
            Assert.assertEquals(TENANT2, restBinding.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQBindingException e) {
            Assert.fail("Failed to clean " + TENANT1);
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        }
    }

    @Test
    public void testDeclareBindingOneToManyExternal() {
        try {
            cleanRabbit(TENANT1);
            CachingConnectionFactory cf = regardsAmqpAdmin.createConnectionFactory(TENANT1);
            rabbitVirtualHostUtils.addVhost(TENANT1, cf);

            Exchange exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY,
                                                                 TENANT1, AmqpCommunicationTarget.EXTERNAL);
            Queue queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT1,
                                                        AmqpCommunicationTarget.EXTERNAL);
            regardsAmqpAdmin.declareBinding(queue, exchange, AmqpCommunicationMode.ONE_TO_MANY, TENANT1);
            List<RestBinding> declaredBindings = retrieveBinding(TENANT1);
            RestBinding restBinding = declaredBindings.get(0);
            Assert.assertEquals(2, declaredBindings.size());
            Assert.assertEquals(TENANT1, restBinding.getVhost());

            cleanRabbit(TENANT2);
            cf = regardsAmqpAdmin.createConnectionFactory(TENANT2);
            rabbitVirtualHostUtils.addVhost(TENANT2, cf);

            exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT2,
                                                        AmqpCommunicationTarget.EXTERNAL);
            queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT2,
                                                  AmqpCommunicationTarget.EXTERNAL);
            regardsAmqpAdmin.declareBinding(queue, exchange, AmqpCommunicationMode.ONE_TO_MANY, TENANT2);
            declaredBindings = retrieveBinding(TENANT2);
            restBinding = declaredBindings.get(0);
            Assert.assertEquals(2, declaredBindings.size());
            Assert.assertEquals(TENANT2, restBinding.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQBindingException e) {
            Assert.fail("Failed to clean " + TENANT1);
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        }
    }

    /**
     * @param pTenant1
     *            tenant we are interested in
     * @return List of {@link RestBinding} to check the result of declareBinding
     * @throws GettingRabbitMQBindingException
     *             any issue that could occur while GETting bindings
     */
    private List<RestBinding> retrieveBinding(String pTenant1) throws GettingRabbitMQBindingException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.AUTHORIZATION, rabbitVirtualHostUtils.setBasic());
        final HttpEntity<Void> request = new HttpEntity<>(headers);
        // CHECKSTYLE:OFF
        final ParameterizedTypeReference<List<RestBinding>> typeRef = new ParameterizedTypeReference<List<RestBinding>>() {

        };
        // CHECKSTYLE:ON
        final ResponseEntity<List<RestBinding>> response = restTemplate
                .exchange(rabbitVirtualHostUtils.getRabbitApiEndpoint() + "/bindings" + SLASH + pTenant1,
                          HttpMethod.GET, request, typeRef);
        final int statusValue = response.getStatusCodeValue();
        if (!rabbitVirtualHostUtils.isSuccess(statusValue)) {
            throw new GettingRabbitMQBindingException("GET binding of " + pTenant1);
        }
        return response.getBody();
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

    @Test
    public void testDeclareExchangeOneToOneInternal() {
        try {
            cleanRabbit(TENANT1);
            CachingConnectionFactory cf = regardsAmqpAdmin.createConnectionFactory(TENANT1);
            rabbitVirtualHostUtils.addVhost(TENANT1, cf);

            Exchange exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE,
                                                                 TENANT1, AmqpCommunicationTarget.INTERNAL);
            List<RestExchange> declaredExchanges = retrieveExchange(TENANT1);
            // TODO: get newly declared exchange not one of the default
            RestExchange restExchange = declaredExchanges.get(0);
            // 1 + all default = 8
            Assert.assertEquals(8, declaredExchanges.size());
            Assert.assertEquals(TENANT1, restExchange.getVhost());

            cleanRabbit(TENANT2);
            cf = regardsAmqpAdmin.createConnectionFactory(TENANT2);
            rabbitVirtualHostUtils.addVhost(TENANT2, cf);

            exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE, TENANT2,
                                                        AmqpCommunicationTarget.INTERNAL);
            declaredExchanges = retrieveExchange(TENANT2);
            // TODO: get newly declared exchange not one of the default
            restExchange = declaredExchanges.get(0);
            // 1 + all default = 8
            Assert.assertEquals(8, declaredExchanges.size());
            Assert.assertEquals(TENANT2, restExchange.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQExchangeException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        }
    }

    @Test
    public void testDeclareExchangeOneToManyInternal() {
        try {
            cleanRabbit(TENANT1);
            CachingConnectionFactory cf = regardsAmqpAdmin.createConnectionFactory(TENANT1);
            rabbitVirtualHostUtils.addVhost(TENANT1, cf);

            Exchange exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY,
                                                                 TENANT1, AmqpCommunicationTarget.INTERNAL);
            List<RestExchange> declaredExchanges = retrieveExchange(TENANT1);
            // TODO: get newly declared exchange not one of the default
            RestExchange restExchange = declaredExchanges.get(0);
            // 1 + all default = 8
            Assert.assertEquals(8, declaredExchanges.size());
            Assert.assertEquals(TENANT1, restExchange.getVhost());

            cleanRabbit(TENANT2);
            cf = regardsAmqpAdmin.createConnectionFactory(TENANT2);
            rabbitVirtualHostUtils.addVhost(TENANT2, cf);

            exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT2,
                                                        AmqpCommunicationTarget.INTERNAL);
            declaredExchanges = retrieveExchange(TENANT2);
            // TODO: get newly declared exchange not one of the default
            restExchange = declaredExchanges.get(0);
            // 1 + all default = 8
            Assert.assertEquals(8, declaredExchanges.size());
            Assert.assertEquals(TENANT2, restExchange.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQExchangeException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        }
    }

    @Test
    public void testDeclareExchangeOneToOneExternal() {
        try {
            cleanRabbit(TENANT1);
            CachingConnectionFactory cf = regardsAmqpAdmin.createConnectionFactory(TENANT1);
            rabbitVirtualHostUtils.addVhost(TENANT1, cf);

            Exchange exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE,
                                                                 TENANT1, AmqpCommunicationTarget.EXTERNAL);
            List<RestExchange> declaredExchanges = retrieveExchange(TENANT1);
            // TODO: get newly declared exchange not one of the default
            RestExchange restExchange = declaredExchanges.get(0);
            // 1 + all default = 8
            Assert.assertEquals(8, declaredExchanges.size());
            Assert.assertEquals(TENANT1, restExchange.getVhost());

            cleanRabbit(TENANT2);
            cf = regardsAmqpAdmin.createConnectionFactory(TENANT2);
            rabbitVirtualHostUtils.addVhost(TENANT2, cf);

            exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE, TENANT2,
                                                        AmqpCommunicationTarget.EXTERNAL);
            declaredExchanges = retrieveExchange(TENANT2);
            // TODO: get newly declared exchange not one of the default
            restExchange = declaredExchanges.get(0);
            // 1 + all default
            Assert.assertEquals(8, declaredExchanges.size());
            Assert.assertEquals(TENANT2, restExchange.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQExchangeException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        }
    }

    @Test
    public void testDeclareExchangeOneToManyExternal() {
        try {
            cleanRabbit(TENANT1);
            CachingConnectionFactory cf = regardsAmqpAdmin.createConnectionFactory(TENANT1);
            rabbitVirtualHostUtils.addVhost(TENANT1, cf);

            Exchange exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY,
                                                                 TENANT1, AmqpCommunicationTarget.EXTERNAL);
            List<RestExchange> declaredExchanges = retrieveExchange(TENANT1);
            // TODO: get newly declared exchange not one of the default
            RestExchange restExchange = declaredExchanges.get(0);
            // 1 + all = 8 default
            Assert.assertEquals(8, declaredExchanges.size());
            Assert.assertEquals(TENANT1, restExchange.getVhost());

            cleanRabbit(TENANT2);
            cf = regardsAmqpAdmin.createConnectionFactory(TENANT2);
            rabbitVirtualHostUtils.addVhost(TENANT2, cf);

            exchange = regardsAmqpAdmin.declareExchange(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT2,
                                                        AmqpCommunicationTarget.EXTERNAL);
            declaredExchanges = retrieveExchange(TENANT2);
            // TODO: get newly declared exchange not one of the default
            restExchange = declaredExchanges.get(0);
            // 1 + all default = 8
            Assert.assertEquals(8, declaredExchanges.size());
            Assert.assertEquals(TENANT2, restExchange.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQExchangeException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        }
    }

    /**
     * @param pTenant2
     *            tenant we are interested in
     * @return List of {@link RestExchange} to check the result of declareExchange
     * @throws GettingRabbitMQExchangeException
     *             any issue that could occur while GETting exchanges
     */
    private List<RestExchange> retrieveExchange(String pTenant2) throws GettingRabbitMQExchangeException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.AUTHORIZATION, rabbitVirtualHostUtils.setBasic());
        final HttpEntity<Void> request = new HttpEntity<>(headers);
        // CHECKSTYLE:OFF
        final ParameterizedTypeReference<List<RestExchange>> typeRef = new ParameterizedTypeReference<List<RestExchange>>() {

        };
        // CHECKSTYLE:ON
        final ResponseEntity<List<RestExchange>> response = restTemplate
                .exchange(rabbitVirtualHostUtils.getRabbitApiEndpoint() + "/exchanges" + SLASH + pTenant2,
                          HttpMethod.GET, request, typeRef);
        final int statusValue = response.getStatusCodeValue();
        if (!rabbitVirtualHostUtils.isSuccess(statusValue)) {
            throw new GettingRabbitMQExchangeException("GET exchanges of " + pTenant2);
        }
        return response.getBody();
    }

    @Test
    public void testDeclareQueueOneToOneInternal() {
        try {
            cleanRabbit(TENANT1);
            CachingConnectionFactory cf = regardsAmqpAdmin.createConnectionFactory(TENANT1);
            rabbitVirtualHostUtils.addVhost(TENANT1, cf);

            Queue queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE, TENANT1,
                                                        AmqpCommunicationTarget.INTERNAL);
            List<RestQueue> declaredQueues = retrieveQueues(TENANT1);
            RestQueue restQueue = declaredQueues.get(0);
            Assert.assertEquals(1, declaredQueues.size());
            Assert.assertEquals(TENANT1, restQueue.getVhost());

            cleanRabbit(TENANT2);
            cf = regardsAmqpAdmin.createConnectionFactory(TENANT2);
            rabbitVirtualHostUtils.addVhost(TENANT2, cf);

            queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE, TENANT2,
                                                  AmqpCommunicationTarget.INTERNAL);
            declaredQueues = retrieveQueues(TENANT2);
            restQueue = declaredQueues.get(0);
            Assert.assertEquals(1, declaredQueues.size());
            Assert.assertEquals(TENANT2, restQueue.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQQueueException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        }
    }

    @Test
    public void testDeclareQueueOneToManyInternal() {
        try {
            cleanRabbit(TENANT1);
            CachingConnectionFactory cf = regardsAmqpAdmin.createConnectionFactory(TENANT1);
            rabbitVirtualHostUtils.addVhost(TENANT1, cf);

            Queue queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT1,
                                                        AmqpCommunicationTarget.INTERNAL);
            List<RestQueue> declaredQueues = retrieveQueues(TENANT1);
            // TODO: get newly declared exchange not one of the default
            RestQueue restQueue = declaredQueues.get(0);
            // TODO: 1 + al default
            Assert.assertEquals(1, declaredQueues.size());
            Assert.assertEquals(TENANT1, restQueue.getVhost());

            cleanRabbit(TENANT2);
            cf = regardsAmqpAdmin.createConnectionFactory(TENANT2);
            rabbitVirtualHostUtils.addVhost(TENANT2, cf);

            queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT2,
                                                  AmqpCommunicationTarget.INTERNAL);
            declaredQueues = retrieveQueues(TENANT2);
            restQueue = declaredQueues.get(0);
            Assert.assertEquals(1, declaredQueues.size());
            Assert.assertEquals(TENANT2, restQueue.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQQueueException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        }
    }

    @Test
    public void testDeclareQueueOneToOneExternal() {
        try {
            cleanRabbit(TENANT1);
            CachingConnectionFactory cf = regardsAmqpAdmin.createConnectionFactory(TENANT1);
            rabbitVirtualHostUtils.addVhost(TENANT1, cf);

            Queue queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE, TENANT1,
                                                        AmqpCommunicationTarget.EXTERNAL);
            List<RestQueue> declaredQueues = retrieveQueues(TENANT1);
            RestQueue restQueue = declaredQueues.get(0);
            Assert.assertEquals(1, declaredQueues.size());
            Assert.assertEquals(TENANT1, restQueue.getVhost());

            cleanRabbit(TENANT2);
            cf = regardsAmqpAdmin.createConnectionFactory(TENANT2);
            rabbitVirtualHostUtils.addVhost(TENANT2, cf);

            queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE, TENANT2,
                                                  AmqpCommunicationTarget.EXTERNAL);
            declaredQueues = retrieveQueues(TENANT2);
            restQueue = declaredQueues.get(0);
            Assert.assertEquals(1, declaredQueues.size());
            Assert.assertEquals(TENANT2, restQueue.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQQueueException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        }
    }

    @Test
    public void testDeclareQueueOneToManyExternal() {
        try {
            cleanRabbit(TENANT1);
            CachingConnectionFactory cf = regardsAmqpAdmin.createConnectionFactory(TENANT1);
            rabbitVirtualHostUtils.addVhost(TENANT1, cf);

            Queue queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT1,
                                                        AmqpCommunicationTarget.EXTERNAL);
            List<RestQueue> declaredQueues = retrieveQueues(TENANT1);
            RestQueue restQueue = declaredQueues.get(0);
            Assert.assertEquals(1, declaredQueues.size());
            Assert.assertEquals(TENANT1, restQueue.getVhost());

            cleanRabbit(TENANT2);
            cf = regardsAmqpAdmin.createConnectionFactory(TENANT2);
            rabbitVirtualHostUtils.addVhost(TENANT2, cf);

            queue = regardsAmqpAdmin.declareQueue(TestEvent.class, AmqpCommunicationMode.ONE_TO_MANY, TENANT2,
                                                  AmqpCommunicationTarget.EXTERNAL);
            declaredQueues = retrieveQueues(TENANT2);
            restQueue = declaredQueues.get(0);
            Assert.assertEquals(1, declaredQueues.size());
            Assert.assertEquals(TENANT2, restQueue.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQQueueException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        }
    }

    @After
    public void clean() {
        try {
            cleanRabbit(TENANT1);
            cleanRabbit(TENANT2);
        } catch (CleaningRabbitMQVhostException e) {
            LOGGER.debug("Issue during cleaning the broker", e);
        }
    }

    /**
     * @param pTenant2
     *            tenant we are interested in
     * @return List of {@link RestQueue} to check the result of declareQueue
     * @throws GettingRabbitMQQueueException
     *             any issue that could occur while GETting queues
     */
    private List<RestQueue> retrieveQueues(String pTenant2) throws GettingRabbitMQQueueException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.AUTHORIZATION, rabbitVirtualHostUtils.setBasic());
        final HttpEntity<Void> request = new HttpEntity<>(headers);
        // CHECKSTYLE:OFF
        final ParameterizedTypeReference<List<RestQueue>> typeRef = new ParameterizedTypeReference<List<RestQueue>>() {

        };
        // CHECKSTYLE:ON
        final ResponseEntity<List<RestQueue>> response = restTemplate
                .exchange(rabbitVirtualHostUtils.getRabbitApiEndpoint() + "/queues" + SLASH + pTenant2, HttpMethod.GET,
                          request, typeRef);
        final int statusValue = response.getStatusCodeValue();
        if (!rabbitVirtualHostUtils.isSuccess(statusValue)) {
            throw new GettingRabbitMQQueueException("GET queues of " + pTenant2);
        }
        return response.getBody();
    }

}
