/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.amqp.testold;

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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import fr.cnes.regards.framework.amqp.Poller;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.AmqpTestsConfiguration;
import fr.cnes.regards.framework.amqp.testold.domain.CleaningRabbitMQVhostException;
import fr.cnes.regards.framework.amqp.testold.domain.TestEvent;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * @author svissier
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AmqpTestsConfiguration.class })
public class PollerIT {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PollerIT.class);

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
    private IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

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
        Assume.assumeTrue(rabbitVirtualHostAdmin.brokerRunning());
        rabbitVirtualHostAdmin.addVhost(TENANT);
    }

    /**
     * test the polling ONE_TO_MANY to message broker
     */
    @Requirement("REGARDS_DSL_CMP_ARC_170")
    @Purpose("test the polling ONE_TO_MANY to message broker")
    @Test
    public void testPollOneToManyExternal() {

        TestEvent toSend;

        try {
            rabbitVirtualHostAdmin.bind(TENANT);

            final Exchange exchange = regardsAmqpAdmin.declareExchange(TENANT, TestEvent.class, WorkerMode.BROADCAST,
                                                                       Target.ALL);
            final Queue queue = regardsAmqpAdmin.declareUnicastQueue(TENANT, TestEvent.class, WorkerMode.BROADCAST, Target.ALL);
            regardsAmqpAdmin.declareBinding(TENANT, queue, exchange, WorkerMode.BROADCAST);

            toSend = new TestEvent("test3");
            final TenantWrapper<TestEvent> sended = new TenantWrapper<TestEvent>(toSend, TENANT);
            LOGGER.info("SENDED " + sended);
            rabbitTemplate.convertAndSend(TestEvent.class.getName(), "", sended);
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }

        final TenantWrapper<TestEvent> wrapperReceived;
        try {
            wrapperReceived = poller.poll(TENANT, TestEvent.class, WorkerMode.BROADCAST, Target.ALL);
            LOGGER.info("=================RECEIVED " + wrapperReceived.getContent());
            final TestEvent received = wrapperReceived.getContent();
            Assert.assertEquals(toSend, received);
        } catch (RabbitMQVhostException e) {
            final String msg = "Polling one to many Test Failed";
            LOGGER.error(msg, e);
            Assert.fail(msg);
        }
        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT));
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

        TestEvent toSend;
        try {
            rabbitVirtualHostAdmin.bind(TENANT);

            final Exchange exchangeOneToOne = regardsAmqpAdmin.declareExchange(TENANT, TestEvent.class,
                                                                               WorkerMode.UNICAST, Target.ALL);
            final Queue queueOneToOne = regardsAmqpAdmin.declareUnicastQueue(TENANT, TestEvent.class, WorkerMode.UNICAST,
                                                                      Target.ALL);
            regardsAmqpAdmin.declareBinding(TENANT, queueOneToOne, exchangeOneToOne, WorkerMode.UNICAST);

            toSend = new TestEvent("test4");
            final TenantWrapper<TestEvent> sended = new TenantWrapper<TestEvent>(toSend, TENANT);
            LOGGER.info("SENDED :" + sended);
            // for one to one communication, exchange is named after the application and not what we are exchanging
            rabbitTemplate.convertAndSend(RegardsAmqpAdmin.REGARDS_NAMESPACE, TestEvent.class.getName(), sended);
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }

        final TenantWrapper<TestEvent> wrapperReceived;
        try {
            wrapperReceived = poller.poll(TENANT, TestEvent.class, WorkerMode.UNICAST, Target.ALL);
            LOGGER.info("=================RECEIVED :" + wrapperReceived.getContent());
            final TestEvent received = wrapperReceived.getContent();
            Assert.assertEquals(toSend, received);
        } catch (RabbitMQVhostException e) {
            final String msg = "Polling one to one Test Failed";
            LOGGER.error(msg, e);
            Assert.fail(msg);
        }

        try {
            cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TENANT));
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
