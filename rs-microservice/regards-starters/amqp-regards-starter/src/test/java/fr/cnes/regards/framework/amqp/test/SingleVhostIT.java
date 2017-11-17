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

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.event.Info;
import fr.cnes.regards.framework.amqp.test.event.InstanceInfo;
import fr.cnes.regards.framework.amqp.test.event.MicroserviceInfo;
import fr.cnes.regards.framework.amqp.test.event.UnicastInfo;
import fr.cnes.regards.framework.amqp.test.handler.AbstractInfoSubscriber;
import fr.cnes.regards.framework.amqp.test.handler.AbstractSubscriber;

/**
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@TestPropertySource(
        properties = { "regards.amqp.management.mode=SINGLE", "regards.tenants=PROJECT, PROJECT1",
                "regards.tenant=PROJECT", "regards.amqp.internal.transaction=true" },
        locations = "classpath:amqp.properties")
public class SingleVhostIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleVhostIT.class);

    @Autowired
    private IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private ISubscriber subscriber;

    @SuppressWarnings("unused")
    @Autowired
    private IPoller poller;

    @Before
    public void init() throws RabbitMQVhostException {
        Assume.assumeTrue(rabbitVirtualHostAdmin.brokerRunning());
    }

    /**
     * Published info is received by single handler
     *
     * <pre>
     * E ----------------> H
     * </pre>
     *
     */
    @Test
    public void publishInfo() {
        AbstractInfoSubscriber infoSubscriber = new AbstractInfoSubscriber() {
        };
        subscriber.subscribeTo(Info.class, infoSubscriber, true);
        publisher.publish(new Info());
        infoSubscriber.assertCount(1);
    }

    @Test
    public void publishMicroserviceInfo() {
        AbstractSubscriber<MicroserviceInfo> infoSubscriber = new AbstractSubscriber<MicroserviceInfo>() {
        };
        subscriber.subscribeTo(MicroserviceInfo.class, infoSubscriber, true);
        publisher.publish(new MicroserviceInfo());
        infoSubscriber.assertCount(1);
    }

    @Test
    public void publishInstanceInfo() {
        AbstractSubscriber<InstanceInfo> infoSubscriber = new AbstractSubscriber<InstanceInfo>() {
        };
        subscriber.subscribeTo(InstanceInfo.class, infoSubscriber, true);
        publisher.publish(new InstanceInfo());
        infoSubscriber.assertCount(1);
    }

    /**
     * Published info is received by multiple handlers
     *
     * <pre>
     * E ----------------> H1
     * |-----------------> H2
     * </pre>
     */
    @Test
    public void publishInfoMultipleReceiver() {
        AbstractInfoSubscriber subscriberOne = new AbstractInfoSubscriber() {
        };
        subscriber.subscribeTo(Info.class, subscriberOne, true);

        AbstractInfoSubscriber subscriberTwo = new AbstractInfoSubscriber() {
        };
        subscriber.subscribeTo(Info.class, subscriberTwo, true);

        String message = "Multiple receivers!";
        publisher.publish(Info.create(message));

        subscriberOne.assertCount(1);
        Assert.assertEquals(message, subscriberOne.getLastInfo().getMessage());

        subscriberTwo.assertCount(1);
        Assert.assertEquals(message, subscriberTwo.getLastInfo().getMessage());
    }

    /**
     * Published info is received by only one handler
     */
    @Test
    public void publishInfoSingleTarget() {
        AbstractSubscriber<UnicastInfo> handler1 = new AbstractSubscriber<UnicastInfo>() {
        };
        subscriber.subscribeTo(UnicastInfo.class, handler1, true);

        AbstractSubscriber<UnicastInfo> handler2 = new AbstractSubscriber<UnicastInfo>() {
        };
        subscriber.subscribeTo(UnicastInfo.class, handler2, true);

        publisher.publish(new UnicastInfo());
        Assert.assertFalse(handler1.checkCount(1) && handler2.checkCount(1));
        Assert.assertTrue(handler1.checkCount(1) || handler2.checkCount(1));
    }
}
