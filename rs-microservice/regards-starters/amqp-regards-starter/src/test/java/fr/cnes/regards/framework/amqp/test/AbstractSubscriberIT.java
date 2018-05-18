/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.amqp.AbstractSubscriber;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.VirtualHostMode;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.event.Info;
import fr.cnes.regards.framework.amqp.test.event.MicroserviceInfo;
import fr.cnes.regards.framework.amqp.test.event.OnePerMicroserviceInfo;
import fr.cnes.regards.framework.amqp.test.event.UnicastInfo;
import fr.cnes.regards.framework.amqp.test.handler.AbstractInfoReceiver;
import fr.cnes.regards.framework.amqp.test.handler.AbstractReceiver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Common subscriber tests for {@link VirtualHostMode#SINGLE} and {@link VirtualHostMode#MULTI} modes
 * @author Marc Sordi
 *
 */
public abstract class AbstractSubscriberIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSubscriberIT.class);

    @Autowired
    protected IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    @Autowired
    protected IPublisher publisher;

    @Autowired
    protected ISubscriber subscriber;

    @Autowired
    protected IAmqpAdmin amqpAdmin;

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
    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Requirement("REGARDS_DSL_CMP_ARC_160")
    @Purpose("Publish and receive a broadcast event without restriction")
    @Test
    public void publishInfo() {
        AbstractInfoReceiver infoSubscriber = new AbstractInfoReceiver() {
        };
        subscriber.subscribeTo(Info.class, infoSubscriber, true);
        publisher.publish(new Info());
        infoSubscriber.assertCount(1);
    }

    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Purpose("Publish and receive a broadcast event with restriction on microservice type")
    @Test
    public void publishMicroserviceInfo() {
        MicroserviceReceiver receiver = new MicroserviceReceiver();
        MicroserviceReceiver receiver2 = new MicroserviceReceiver();
        subscriber.subscribeTo(MicroserviceInfo.class, receiver, true);
        subscriber.subscribeTo(MicroserviceInfo.class, receiver2, true);
        publisher.publish(new MicroserviceInfo());

        Assert.assertFalse(receiver.checkCount(1) && receiver2.checkCount(1));
        Assert.assertTrue(receiver.checkCount(1) || receiver2.checkCount(1));
    }

    private class MicroserviceReceiver extends AbstractReceiver<MicroserviceInfo> {
    }

    /**
     * Published info is received by multiple handlers
     *
     * <pre>
     * E ----------------> H1
     * |-----------------> H2
     * </pre>
     */
    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Purpose("Publish a broadcast event received by multiple receivers")
    @Test
    public void publishInfoMultipleReceiver() {
        AbstractInfoReceiver subscriberOne = new AbstractInfoReceiver() {
        };
        subscriber.subscribeTo(Info.class, subscriberOne, true);

        AbstractInfoReceiver subscriberTwo = new AbstractInfoReceiver() {
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
    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Purpose("Publish a unicast event with multiple receivers but received by only one")
    @Test
    public void publishInfoSingleTarget() {
        AbstractReceiver<UnicastInfo> handler1 = new AbstractReceiver<UnicastInfo>() {
        };
        subscriber.subscribeTo(UnicastInfo.class, handler1, true);

        AbstractReceiver<UnicastInfo> handler2 = new AbstractReceiver<UnicastInfo>() {
        };
        subscriber.subscribeTo(UnicastInfo.class, handler2, true);

        publisher.publish(new UnicastInfo());
        Assert.assertFalse(handler1.checkCount(1) && handler2.checkCount(1));
        Assert.assertTrue(handler1.checkCount(1) || handler2.checkCount(1));
    }

    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Purpose("Subscribing is reentrant and does not imply listener duplication")
    @Test
    public void multipleSubscriptions() {

        AbstractSubscriber abstractSubscriber = (AbstractSubscriber) subscriber;

        // First subscription
        Receiver receiver = new Receiver();
        subscriber.subscribeTo(Info.class, receiver, true);

        // Retrieve listener
        Map<String, SimpleMessageListenerContainer> listeners = abstractSubscriber.getListeners(receiver);
        Assert.assertNotNull(listeners);
        Map<String, Integer> refListeners = new HashMap<>();
        listeners.forEach((k, v) -> refListeners.put(k, v.hashCode()));

        // Second subscription
        subscriber.subscribeTo(Info.class, receiver);

        // Retrieve listener
        Map<String, SimpleMessageListenerContainer> listeners2 = abstractSubscriber.getListeners(receiver);
        Assert.assertNotNull(listeners);
        listeners2.forEach((k, v) -> Assert.assertTrue(refListeners.get(k) == v.hashCode()));
    }

    private class Receiver extends AbstractReceiver<Info> {
    }

    @Test
    public void onePerMicroserviceTypeTest() {

        RegardsAmqpAdmin admin = (RegardsAmqpAdmin) amqpAdmin;

        // Microservice A
        admin.setMicroserviceTypeId("A");
        // Simulate consumer A1
        admin.setMicroserviceInstanceId("A1");
        SingleReceiverA a1 = new SingleReceiverA();
        subscriber.subscribeTo(OnePerMicroserviceInfo.class, a1);
        // Simulate consumer A2
        admin.setMicroserviceInstanceId("A2");
        SingleReceiverA a2 = new SingleReceiverA();
        subscriber.subscribeTo(OnePerMicroserviceInfo.class, a2);

        // Microservice B
        admin.setMicroserviceTypeId("B");
        // Simulate consumer A1
        admin.setMicroserviceInstanceId("B1");
        SingleReceiverB b1 = new SingleReceiverB();
        subscriber.subscribeTo(OnePerMicroserviceInfo.class, b1);
        // Simulate consumer A2
        admin.setMicroserviceInstanceId("B2");
        SingleReceiverB b2 = new SingleReceiverB();
        subscriber.subscribeTo(OnePerMicroserviceInfo.class, b2);

        // Check only one consumer per microservice type receives the event.
        publisher.publish(new OnePerMicroserviceInfo());
        Assert.assertFalse(a1.checkCount(1) && a2.checkCount(1));
        Assert.assertTrue(a1.checkCount(1) || a2.checkCount(1));
        Assert.assertFalse(b1.checkCount(1) && b2.checkCount(1));
        Assert.assertTrue(b1.checkCount(1) || b2.checkCount(1));
    }

    private class SingleReceiverA extends AbstractReceiver<OnePerMicroserviceInfo> {
    }

    private class SingleReceiverB extends AbstractReceiver<OnePerMicroserviceInfo> {
    }
}
