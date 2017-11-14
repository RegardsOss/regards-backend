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
package fr.cnes.regards.framework.amqp.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import fr.cnes.regards.framework.amqp.Subscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import fr.cnes.regards.framework.amqp.test.domain.TestEvent;
import fr.cnes.regards.framework.amqp.test.domain.TestReceiver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

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

/**
 * @author svissier
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AmqpTestsConfiguration.class })
public class SubscriberIT {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberIT.class);

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
    private IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    /**
     * initialization ran before each test case
     */
    @Before
    public void init() {
        Assume.assumeTrue(rabbitVirtualHostAdmin.brokerRunning());
        rabbitVirtualHostAdmin.addVhost(tenant);
        receiverOneToMany = new TestReceiver();
        subscriberOneToManyExternal.subscribeTo(TestEvent.class, receiverOneToMany, WorkerMode.BROADCAST, Target.ALL);
    }

    @After
    public void clean() {
        subscriberOneToManyExternal.unsubscribeFrom(TestEvent.class);
        rabbitVirtualHostAdmin.removeVhost(tenant);
    }

    /**
     * test the subscription to message broker
     */
    @Requirement("REGARDS_DSL_CMP_ARC_160")
    @Purpose("test the subscription to message broker")
    @Test
    public void testSubscribeToOneToManyExternal() {
        final TestEvent toSend = new TestEvent("test one to many");
        final TenantWrapper<TestEvent> sended = new TenantWrapper<TestEvent>(toSend, tenant);
        LOGGER.info(SENDED + sended);

        try {
            rabbitVirtualHostAdmin.bind(tenant);
            // CHECKSTYLE:OFF
            rabbitTemplate.convertAndSend(amqpConfiguration.getExchangeName(TestEvent.class.getName(), Target.ALL),
                                          amqpConfiguration.getRoutingKey("", WorkerMode.BROADCAST), sended, pMessage -> {
                                              final MessageProperties propertiesWithPriority = pMessage
                                                      .getMessageProperties();
                                              propertiesWithPriority.setPriority(0);
                                              return new Message(pMessage.getBody(), propertiesWithPriority);
                                          });
            // CHECKSTYLE:ON
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }

        try {
            Thread.sleep(SLEEP_TIME); // NOSONAR : passive test so we have to wait for the message to be sent
        } catch (InterruptedException e) {
            LOGGER.error(SLEEP_FAIL, e);
            Assert.fail(SLEEP_FAIL);
        }
        LOGGER.info(RECEIVED + receiverOneToMany.getMessage());
        Assert.assertEquals(toSend, receiverOneToMany.getMessage());

    }
}
