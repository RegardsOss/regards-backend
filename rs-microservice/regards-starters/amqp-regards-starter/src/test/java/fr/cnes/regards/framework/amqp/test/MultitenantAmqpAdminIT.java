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

import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.MultitenantAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.RabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.domain.CleaningRabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.domain.GettingRabbitMQBindingException;
import fr.cnes.regards.framework.amqp.test.domain.GettingRabbitMQExchangeException;
import fr.cnes.regards.framework.amqp.test.domain.GettingRabbitMQQueueException;
import fr.cnes.regards.framework.amqp.test.domain.RestBinding;
import fr.cnes.regards.framework.amqp.test.domain.RestExchange;
import fr.cnes.regards.framework.amqp.test.domain.RestQueue;
import fr.cnes.regards.framework.amqp.test.domain.TestEvent;

/**
 * @author svissier
 *
 */
@ActiveProfiles("rabbit")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class MultitenantAmqpAdminIT {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MultitenantAmqpAdminIT.class);

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
    private MultitenantAmqpAdmin regardsAmqpAdmin;

    /**
     * bean helping us to test
     */
    @Autowired
    private IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    /**
     * bean allowing us to clena the broker
     */
    @Autowired
    private RestTemplate restTemplate;

    @Before
    public void init() {
        Assume.assumeTrue(rabbitVirtualHostAdmin.brokerRunning());
    }

    @Test
    public void testDeclareBindingOneToOneInternal() {
        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));
            rabbitVirtualHostAdmin.addVhost(TENANT1);

            List<RestBinding> baseDeclaredBindings = retrieveBinding(TENANT1);
            Exchange exchange = regardsAmqpAdmin.declareExchange(TENANT1, TestEvent.class, WorkerMode.SINGLE,
                                                                 Target.MICROSERVICE);
            Queue queue = regardsAmqpAdmin.declareQueue(TENANT1, TestEvent.class, WorkerMode.SINGLE,
                                                        Target.MICROSERVICE);
            regardsAmqpAdmin.declareBinding(TENANT1, queue, exchange, WorkerMode.SINGLE);
            List<RestBinding> declaredBindings = retrieveBinding(TENANT1);
            RestBinding restBinding = declaredBindings.get(0);
            // because of default amqp exhange on rabbitMQ, it adds 2 binding and not only 1
            // Assert.assertEquals(baseDeclaredBindings.size() + 2, declaredBindings.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT1), restBinding.getVhost());

            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            rabbitVirtualHostAdmin.addVhost(TENANT2);

            baseDeclaredBindings = retrieveBinding(TENANT2);
            exchange = regardsAmqpAdmin.declareExchange(TENANT2, TestEvent.class, WorkerMode.SINGLE,
                                                        Target.MICROSERVICE);
            queue = regardsAmqpAdmin.declareQueue(TENANT2, TestEvent.class, WorkerMode.SINGLE, Target.MICROSERVICE);
            regardsAmqpAdmin.declareBinding(TENANT2, queue, exchange, WorkerMode.SINGLE);
            declaredBindings = retrieveBinding(TENANT2);
            restBinding = declaredBindings.get(0);
            // Assert.assertEquals(baseDeclaredBindings.size() + 2, declaredBindings.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT2), restBinding.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQBindingException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        } finally {
            try {
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            } catch (CleaningRabbitMQVhostException e) {
                LOGGER.debug("Issue during cleaning the broker", e);
            }
        }
    }

    @Test
    public void testDeclareBindingOneToManyInternal() {
        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));

            rabbitVirtualHostAdmin.addVhost(TENANT1);

            List<RestBinding> baseDeclaredBindings = retrieveBinding(TENANT1);
            Exchange exchange = regardsAmqpAdmin.declareExchange(TENANT1, TestEvent.class, WorkerMode.ALL,
                                                                 Target.MICROSERVICE);
            Queue queue = regardsAmqpAdmin.declareQueue(TENANT1, TestEvent.class, WorkerMode.ALL, Target.MICROSERVICE);
            regardsAmqpAdmin.declareBinding(TENANT1, queue, exchange, WorkerMode.ALL);
            List<RestBinding> declaredBindings = retrieveBinding(TENANT1);
            RestBinding restBinding = declaredBindings.get(0);
            // because of default exhange
            // Assert.assertEquals(baseDeclaredBindings.size() + 1, declaredBindings.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT1), restBinding.getVhost());

            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            rabbitVirtualHostAdmin.addVhost(TENANT2);

            baseDeclaredBindings = retrieveBinding(TENANT2);
            exchange = regardsAmqpAdmin.declareExchange(TENANT2, TestEvent.class, WorkerMode.ALL, Target.MICROSERVICE);
            queue = regardsAmqpAdmin.declareQueue(TENANT2, TestEvent.class, WorkerMode.ALL, Target.MICROSERVICE);
            regardsAmqpAdmin.declareBinding(TENANT2, queue, exchange, WorkerMode.ALL);
            declaredBindings = retrieveBinding(TENANT2);
            restBinding = declaredBindings.get(0);
            // because of default exchange
            // Assert.assertEquals(baseDeclaredBindings.size() + 1, declaredBindings.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT2), restBinding.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQBindingException e) {
            Assert.fail("Failed to clean " + TENANT1);
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        } finally {
            try {
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            } catch (CleaningRabbitMQVhostException e) {
                LOGGER.debug("Issue during cleaning the broker", e);
            }
        }
    }

    @Test
    public void testDeclareBindingOneToOneExternal() {
        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));
            rabbitVirtualHostAdmin.addVhost(TENANT1);

            Exchange exchange = regardsAmqpAdmin.declareExchange(TENANT1, TestEvent.class, WorkerMode.SINGLE,
                                                                 Target.ALL);
            Queue queue = regardsAmqpAdmin.declareQueue(TENANT1, TestEvent.class, WorkerMode.SINGLE, Target.ALL);
            regardsAmqpAdmin.declareBinding(TENANT1, queue, exchange, WorkerMode.SINGLE);
            List<RestBinding> declaredBindings = retrieveBinding(TENANT1);
            RestBinding restBinding = declaredBindings.get(0);
            // Assert.assertEquals(7, declaredBindings.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT1), restBinding.getVhost());

            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            rabbitVirtualHostAdmin.addVhost(TENANT2);

            exchange = regardsAmqpAdmin.declareExchange(TENANT2, TestEvent.class, WorkerMode.SINGLE, Target.ALL);
            queue = regardsAmqpAdmin.declareQueue(TENANT2, TestEvent.class, WorkerMode.SINGLE, Target.ALL);
            regardsAmqpAdmin.declareBinding(TENANT2, queue, exchange, WorkerMode.SINGLE);
            declaredBindings = retrieveBinding(TENANT2);
            restBinding = declaredBindings.get(0);
            // Assert.assertEquals(7, declaredBindings.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT2), restBinding.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQBindingException e) {
            Assert.fail("Failed to clean " + TENANT1);
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        } finally {
            try {
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            } catch (CleaningRabbitMQVhostException e) {
                LOGGER.debug("Issue during cleaning the broker", e);
            }
        }
    }

    @Test
    public void testDeclareBindingOneToManyExternal() {
        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));

            rabbitVirtualHostAdmin.addVhost(TENANT1);

            Exchange exchange = regardsAmqpAdmin.declareExchange(TENANT1, TestEvent.class, WorkerMode.ALL, Target.ALL);
            Queue queue = regardsAmqpAdmin.declareQueue(TENANT1, TestEvent.class, WorkerMode.ALL, Target.ALL);
            regardsAmqpAdmin.declareBinding(TENANT1, queue, exchange, WorkerMode.ALL);
            List<RestBinding> declaredBindings = retrieveBinding(TENANT1);
            RestBinding restBinding = declaredBindings.get(0);
            // Assert.assertEquals(4, declaredBindings.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT1), restBinding.getVhost());

            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            rabbitVirtualHostAdmin.addVhost(TENANT2);

            exchange = regardsAmqpAdmin.declareExchange(TENANT2, TestEvent.class, WorkerMode.ALL, Target.ALL);
            queue = regardsAmqpAdmin.declareQueue(TENANT2, TestEvent.class, WorkerMode.ALL, Target.ALL);
            regardsAmqpAdmin.declareBinding(TENANT2, queue, exchange, WorkerMode.ALL);
            declaredBindings = retrieveBinding(TENANT2);
            restBinding = declaredBindings.get(0);
            // Assert.assertEquals(4, declaredBindings.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT2), restBinding.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQBindingException e) {
            Assert.fail("Failed to clean " + TENANT1);
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        } finally {
            try {
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            } catch (CleaningRabbitMQVhostException e) {
                LOGGER.debug("Issue during cleaning the broker", e);
            }
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
        headers.add(HttpHeaders.AUTHORIZATION, rabbitVirtualHostAdmin.setBasic());
        final HttpEntity<Void> request = new HttpEntity<>(headers);
        // CHECKSTYLE:OFF
        final ParameterizedTypeReference<List<RestBinding>> typeRef = new ParameterizedTypeReference<List<RestBinding>>() {

        };
        // CHECKSTYLE:ON
        final ResponseEntity<List<RestBinding>> response = restTemplate
                .exchange(rabbitVirtualHostAdmin.getRabbitApiEndpoint() + "/bindings" + SLASH
                        + RabbitVirtualHostAdmin.getVhostName(pTenant1), HttpMethod.GET, request, typeRef);
        final int statusValue = response.getStatusCodeValue();
        if (!rabbitVirtualHostAdmin.isSuccess(statusValue)) {
            throw new GettingRabbitMQBindingException("GET binding of " + pTenant1);
        }
        return response.getBody();
    }

    /**
     * delete the virtual host if existing
     *
     * @param pTenant
     *            tenant to clean
     * @throws CleaningRabbitMQVhostException
     *             any issues that could occur
     */
    private void cleanRabbit(String pTenant) throws CleaningRabbitMQVhostException {
        final List<String> existingVhost = rabbitVirtualHostAdmin.retrieveVhostList();
        final String vhostName = RabbitVirtualHostAdmin.getVhostName(pTenant);
        if (existingVhost.stream().filter(vhost -> vhost.equals(vhostName)).findAny().isPresent()) {
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.AUTHORIZATION, rabbitVirtualHostAdmin.setBasic());
            final HttpEntity<Void> request = new HttpEntity<>(headers);
            final ResponseEntity<String> response = restTemplate
                    .exchange(rabbitVirtualHostAdmin.getRabbitApiVhostEndpoint() + SLASH + vhostName, HttpMethod.DELETE,
                              request, String.class);
            final int statusValue = response.getStatusCodeValue();
            // if successful or 404 then the broker is clean
            if (!(rabbitVirtualHostAdmin.isSuccess(statusValue) || (statusValue == HttpStatus.NOT_FOUND.value()))) {
                throw new CleaningRabbitMQVhostException(response.getBody());
            }
        }
    }

    @Test
    public void testDeclareExchangeOneToOneInternal() {
        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));

            rabbitVirtualHostAdmin.addVhost(TENANT1);

            Exchange exchange = regardsAmqpAdmin.declareExchange(TENANT1, TestEvent.class, WorkerMode.SINGLE,
                                                                 Target.MICROSERVICE);
            List<RestExchange> declaredExchanges = retrieveExchange(TENANT1);
            // TODO: get newly declared exchange not one of the default
            RestExchange restExchange = declaredExchanges.get(0);
            // 1 + all default = 11
            // Assert.assertEquals(11, declaredExchanges.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT1), restExchange.getVhost());

            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));

            rabbitVirtualHostAdmin.addVhost(TENANT2);

            exchange = regardsAmqpAdmin.declareExchange(TENANT2, TestEvent.class, WorkerMode.SINGLE,
                                                        Target.MICROSERVICE);
            declaredExchanges = retrieveExchange(TENANT2);
            // TODO: get newly declared exchange not one of the default
            restExchange = declaredExchanges.get(0);
            // 1 + all default = 11
            // Assert.assertEquals(11, declaredExchanges.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT2), restExchange.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQExchangeException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        } finally {
            try {
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            } catch (CleaningRabbitMQVhostException e) {
                LOGGER.debug("Issue during cleaning the broker", e);
            }
        }
    }

    @Test
    public void testDeclareExchangeOneToManyInternal() {
        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));

            rabbitVirtualHostAdmin.addVhost(TENANT1);

            List<RestExchange> baseDeclaredExchanges = retrieveExchange(TENANT1);
            Exchange exchange = regardsAmqpAdmin.declareExchange(TENANT1, TestEvent.class, WorkerMode.ALL,
                                                                 Target.MICROSERVICE);
            List<RestExchange> declaredExchanges = retrieveExchange(TENANT1);

            RestExchange restExchange = declaredExchanges.get(0);

            // Assert.assertEquals(baseDeclaredExchanges.size() + 1, declaredExchanges.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT1), restExchange.getVhost());

            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));

            rabbitVirtualHostAdmin.addVhost(TENANT2);

            baseDeclaredExchanges = retrieveExchange(TENANT2);
            exchange = regardsAmqpAdmin.declareExchange(TENANT2, TestEvent.class, WorkerMode.ALL, Target.MICROSERVICE);
            declaredExchanges = retrieveExchange(TENANT2);

            restExchange = declaredExchanges.get(0);

            // Assert.assertEquals(baseDeclaredExchanges.size() + 1, declaredExchanges.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT2), restExchange.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQExchangeException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        } finally {
            try {
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            } catch (CleaningRabbitMQVhostException e) {
                LOGGER.debug("Issue during cleaning the broker", e);
            }
        }
    }

    @Test
    public void testDeclareExchangeOneToOneExternal() {
        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));

            rabbitVirtualHostAdmin.addVhost(TENANT1);

            Exchange exchange = regardsAmqpAdmin.declareExchange(TENANT1, TestEvent.class, WorkerMode.SINGLE,
                                                                 Target.ALL);
            List<RestExchange> declaredExchanges = retrieveExchange(TENANT1);
            // TODO: get newly declared exchange not one of the default
            RestExchange restExchange = declaredExchanges.get(0);
            // 1 + all default = 11
            // Assert.assertEquals(11, declaredExchanges.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT1), restExchange.getVhost());

            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));

            rabbitVirtualHostAdmin.addVhost(TENANT2);

            exchange = regardsAmqpAdmin.declareExchange(TENANT2, TestEvent.class, WorkerMode.SINGLE, Target.ALL);
            declaredExchanges = retrieveExchange(TENANT2);
            // TODO: get newly declared exchange not one of the default
            restExchange = declaredExchanges.get(0);
            // 1 + all default
            // Assert.assertEquals(11, declaredExchanges.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT2), restExchange.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQExchangeException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        } finally {
            try {
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            } catch (CleaningRabbitMQVhostException e) {
                LOGGER.debug("Issue during cleaning the broker", e);
            }
        }
    }

    @Test
    public void testDeclareExchangeOneToManyExternal() {
        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));

            rabbitVirtualHostAdmin.addVhost(TENANT1);

            Exchange exchange = regardsAmqpAdmin.declareExchange(TENANT1, TestEvent.class, WorkerMode.ALL, Target.ALL);
            List<RestExchange> declaredExchanges = retrieveExchange(TENANT1);
            // TODO: get newly declared exchange not one of the default
            RestExchange restExchange = declaredExchanges.get(0);
            // 1 + all = 8 default
            // Assert.assertEquals(11, declaredExchanges.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT1), restExchange.getVhost());

            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));

            rabbitVirtualHostAdmin.addVhost(TENANT2);

            exchange = regardsAmqpAdmin.declareExchange(TENANT2, TestEvent.class, WorkerMode.ALL, Target.ALL);
            declaredExchanges = retrieveExchange(TENANT2);
            // TODO: get newly declared exchange not one of the default
            restExchange = declaredExchanges.get(0);
            // 1 + all default = 8
            // Assert.assertEquals(11, declaredExchanges.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT2), restExchange.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQExchangeException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        } finally {
            try {
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            } catch (CleaningRabbitMQVhostException e) {
                LOGGER.debug("Issue during cleaning the broker", e);
            }
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
        headers.add(HttpHeaders.AUTHORIZATION, rabbitVirtualHostAdmin.setBasic());
        final HttpEntity<Void> request = new HttpEntity<>(headers);
        // CHECKSTYLE:OFF
        final ParameterizedTypeReference<List<RestExchange>> typeRef = new ParameterizedTypeReference<List<RestExchange>>() {

        };
        // CHECKSTYLE:ON
        final ResponseEntity<List<RestExchange>> response = restTemplate
                .exchange(rabbitVirtualHostAdmin.getRabbitApiEndpoint() + "/exchanges" + SLASH
                        + RabbitVirtualHostAdmin.getVhostName(pTenant2), HttpMethod.GET, request, typeRef);
        final int statusValue = response.getStatusCodeValue();
        if (!rabbitVirtualHostAdmin.isSuccess(statusValue)) {
            throw new GettingRabbitMQExchangeException("GET exchanges of " + pTenant2);
        }
        return response.getBody();
    }

    @Test
    public void testDeclareQueueOneToOneInternal() {
        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));

            rabbitVirtualHostAdmin.addVhost(TENANT1);

            Queue queue = regardsAmqpAdmin.declareQueue(TENANT1, TestEvent.class, WorkerMode.SINGLE,
                                                        Target.MICROSERVICE);
            List<RestQueue> declaredQueues = retrieveQueues(TENANT1);
            RestQueue restQueue = declaredQueues.get(0);
            // Assert.assertEquals(1, declaredQueues.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT1), restQueue.getVhost());

            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));

            rabbitVirtualHostAdmin.addVhost(TENANT2);

            queue = regardsAmqpAdmin.declareQueue(TENANT2, TestEvent.class, WorkerMode.SINGLE, Target.MICROSERVICE);
            declaredQueues = retrieveQueues(TENANT2);
            restQueue = declaredQueues.get(0);
            // Assert.assertEquals(1, declaredQueues.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT2), restQueue.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQQueueException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        } finally {
            try {
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            } catch (CleaningRabbitMQVhostException e) {
                LOGGER.debug("Issue during cleaning the broker", e);
            }
        }
    }

    @Test
    public void testDeclareQueueOneToManyInternal() {
        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));

            rabbitVirtualHostAdmin.addVhost(TENANT1);

            Queue queue = regardsAmqpAdmin.declareQueue(TENANT1, TestEvent.class, WorkerMode.ALL, Target.MICROSERVICE);
            List<RestQueue> declaredQueues = retrieveQueues(TENANT1);
            // TODO: get newly declared exchange not one of the default
            RestQueue restQueue = declaredQueues.get(0);
            // TODO: 1 + al default
            // Assert.assertEquals(3, declaredQueues.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT1), restQueue.getVhost());

            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));

            rabbitVirtualHostAdmin.addVhost(TENANT2);

            queue = regardsAmqpAdmin.declareQueue(TENANT2, TestEvent.class, WorkerMode.ALL, Target.MICROSERVICE);
            declaredQueues = retrieveQueues(TENANT2);
            restQueue = declaredQueues.get(0);
            // Assert.assertEquals(3, declaredQueues.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT2), restQueue.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQQueueException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        } finally {
            try {
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            } catch (CleaningRabbitMQVhostException e) {
                LOGGER.debug("Issue during cleaning the broker", e);
            }
        }
    }

    @Test
    public void testDeclareQueueOneToOneExternal() {
        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));

            rabbitVirtualHostAdmin.addVhost(TENANT1);

            Queue queue = regardsAmqpAdmin.declareQueue(TENANT1, TestEvent.class, WorkerMode.SINGLE, Target.ALL);
            List<RestQueue> declaredQueues = retrieveQueues(TENANT1);
            RestQueue restQueue = declaredQueues.get(0);
            // Assert.assertEquals(4, declaredQueues.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT1), restQueue.getVhost());

            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));

            rabbitVirtualHostAdmin.addVhost(TENANT2);

            queue = regardsAmqpAdmin.declareQueue(TENANT2, TestEvent.class, WorkerMode.SINGLE, Target.ALL);
            declaredQueues = retrieveQueues(TENANT2);
            restQueue = declaredQueues.get(0);
            // Assert.assertEquals(4, declaredQueues.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT2), restQueue.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQQueueException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        } finally {
            try {
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            } catch (CleaningRabbitMQVhostException e) {
                LOGGER.debug("Issue during cleaning the broker", e);
            }
        }
    }

    @Test
    public void testDeclareQueueOneToManyExternal() {
        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));

            rabbitVirtualHostAdmin.addVhost(TENANT1);

            Queue queue = regardsAmqpAdmin.declareQueue(TENANT1, TestEvent.class, WorkerMode.ALL, Target.ALL);
            List<RestQueue> declaredQueues = retrieveQueues(TENANT1);
            RestQueue restQueue = declaredQueues.get(0);
            // Assert.assertEquals(4, declaredQueues.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT1), restQueue.getVhost());

            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));

            rabbitVirtualHostAdmin.addVhost(TENANT2);

            queue = regardsAmqpAdmin.declareQueue(TENANT2, TestEvent.class, WorkerMode.ALL, Target.ALL);
            declaredQueues = retrieveQueues(TENANT2);
            restQueue = declaredQueues.get(0);
            // Assert.assertEquals(4, declaredQueues.size());
            Assert.assertEquals(RabbitVirtualHostAdmin.getVhostName(TENANT2), restQueue.getVhost());

        } catch (CleaningRabbitMQVhostException | GettingRabbitMQQueueException e) {
            Assert.fail("Failed to clean Tenant");
        } catch (RabbitMQVhostException e) {
            Assert.fail("Failed to add virtualhost " + TENANT1);
        } finally {
            try {
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT1));
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT2));
            } catch (CleaningRabbitMQVhostException e) {
                LOGGER.debug("Issue during cleaning the broker", e);
            }
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
        headers.add(HttpHeaders.AUTHORIZATION, rabbitVirtualHostAdmin.setBasic());
        final HttpEntity<Void> request = new HttpEntity<>(headers);
        // CHECKSTYLE:OFF
        final ParameterizedTypeReference<List<RestQueue>> typeRef = new ParameterizedTypeReference<List<RestQueue>>() {

        };
        // CHECKSTYLE:ON
        final ResponseEntity<List<RestQueue>> response = restTemplate
                .exchange(rabbitVirtualHostAdmin.getRabbitApiEndpoint() + "/queues" + SLASH
                        + RabbitVirtualHostAdmin.getVhostName(pTenant2), HttpMethod.GET, request, typeRef);
        final int statusValue = response.getStatusCodeValue();
        if (!rabbitVirtualHostAdmin.isSuccess(statusValue)) {
            throw new GettingRabbitMQQueueException("GET queues of " + pTenant2);
        }
        return response.getBody();
    }

}
