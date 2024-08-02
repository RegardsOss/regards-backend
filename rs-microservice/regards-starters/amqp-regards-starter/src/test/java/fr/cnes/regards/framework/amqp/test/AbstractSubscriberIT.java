/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.AbstractSubscriber;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.*;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.event.*;
import fr.cnes.regards.framework.amqp.test.handler.AbstractInfoReceiver;
import fr.cnes.regards.framework.amqp.test.handler.AbstractReceiver;
import fr.cnes.regards.framework.amqp.test.handler.GsonInfoHandler;
import fr.cnes.regards.framework.amqp.test.handler.GsonInfoNoWrapperHandler;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Common subscriber tests for {@link VirtualHostMode#SINGLE} and {@link VirtualHostMode#MULTI} modes
 *
 * @author Marc Sordi
 */
@ActiveProfiles("test")
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

    @Autowired
    private RabbitTemplate rabbitTemplate;

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
     */
    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Requirement("REGARDS_DSL_CMP_ARC_160")
    @Purpose("Publish and receive a broadcast event without restriction")
    @Test
    public void publishInfo() {
        // Given
        AbstractInfoReceiver infoSubscriber = new AbstractInfoReceiver() {

        };
        subscriber.subscribeTo(Info.class, infoSubscriber, true);

        NotificationRequestEventReceiver notificationRequestEventReceiver = new NotificationRequestEventReceiver() {

        };
        subscriber.subscribeTo(NotificationRequestEvent.class, notificationRequestEventReceiver, true);
        // When
        Info info = new Info();
        publisher.publish(info);
        // Then
        infoSubscriber.assertCount(1);
        Assert.assertEquals(info.getMessage(), infoSubscriber.getLastInfo().getMessage());

        notificationRequestEventReceiver.assertCount(1);
        Assert.assertNotNull(notificationRequestEventReceiver.getLastInfo().getMetadata());
        Assert.assertNotNull(notificationRequestEventReceiver.getLastInfo().getPayload());
    }

    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Requirement("REGARDS_DSL_CMP_ARC_160")
    @Purpose("Publish and receive a broadcast event without restriction with GSON message converter")
    @Test
    public void publishInfoWithGson() {
        // Given
        GsonInfoHandler handler = new GsonInfoHandler();
        subscriber.subscribeTo(GsonInfo.class, handler, true);

        NotificationRequestEventReceiver notificationRequestEventReceiver = new NotificationRequestEventReceiver() {

        };
        subscriber.subscribeTo(NotificationRequestEvent.class, notificationRequestEventReceiver, true);

        // When
        publisher.publish(new GsonInfo());
        // Then
        handler.assertCount(1);

        notificationRequestEventReceiver.assertCount(0);
    }

    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Requirement("REGARDS_DSL_CMP_ARC_160")
    @Purpose("Publish and receive a broadcast event without restriction with GSON message converter")
    @Test
    public void publishInfoNoWrapperWithGson() {
        // Given
        GsonInfoNoWrapperHandler handler = new GsonInfoNoWrapperHandler();
        subscriber.subscribeTo(GsonInfo.class, handler, true);
        // When
        publisher.publish(new GsonInfo());
        // Then
        handler.assertCount(1);
    }

    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Purpose("Publish and receive a broadcast event with restriction on microservice type")
    @Test
    public void publishMicroserviceInfo() {
        // Given
        MicroserviceReceiver receiver = new MicroserviceReceiver();
        MicroserviceReceiver receiver2 = new MicroserviceReceiver();
        subscriber.subscribeTo(MicroserviceInfo.class, receiver, true);
        subscriber.subscribeTo(MicroserviceInfo.class, receiver2, true);
        // When
        publisher.publish(new MicroserviceInfo());
        // Then
        Assert.assertFalse(receiver.checkCount(1) && receiver2.checkCount(1));
        Assert.assertTrue(receiver.checkCount(1) || receiver2.checkCount(1));
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
        // Given
        AbstractInfoReceiver subscriberOne = new AbstractInfoReceiver() {

        };
        subscriber.subscribeTo(Info.class, subscriberOne, true);

        AbstractInfoReceiver subscriberTwo = new AbstractInfoReceiver() {

        };
        subscriber.subscribeTo(Info.class, subscriberTwo, true);

        String message = "Multiple receivers!";
        // When
        publisher.publish(Info.create(message));
        // Then
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
        // Given
        AbstractReceiver<UnicastInfo> handler1 = new AbstractReceiver<UnicastInfo>() {

        };
        subscriber.subscribeTo(UnicastInfo.class, handler1, true);

        AbstractReceiver<UnicastInfo> handler2 = new AbstractReceiver<UnicastInfo>() {

        };
        subscriber.subscribeTo(UnicastInfo.class, handler2, true);
        // When
        publisher.publish(new UnicastInfo());
        // Then
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
        listeners2.forEach((k, v) -> Assert.assertEquals((int) refListeners.get(k), v.hashCode()));
    }

    @Test
    public void customQueueNameUnicastTest() throws InterruptedException {
        // Given
        String queueName = "test.queue.customQueueNameUnicastTest";
        String queueName2 = "test.queue2.customQueueNameUnicastTest";
        String exchangeName = "test.exchange.customQueueNameUnicastTest";

        AbstractReceiver<UnicastInfo> handler1 = new AbstractReceiver<UnicastInfo>() {

        };
        subscriber.subscribeTo(UnicastInfo.class, handler1, queueName, exchangeName);

        AbstractReceiver<UnicastInfo> handler2 = new AbstractReceiver<UnicastInfo>() {

        };
        subscriber.subscribeTo(UnicastInfo.class, handler2, queueName2, exchangeName);
        // When
        publisher.publish(new UnicastInfo(), exchangeName, Optional.of(queueName));
        // Then
        Assert.assertFalse(handler1.checkCount(1) && handler2.checkCount(1));
        Assert.assertTrue(handler1.checkCount(1) || handler2.checkCount(1));
    }

    @Test
    public void customQueueNameBroadcastAllTest() throws InterruptedException {
        // Given
        String queueName = "test.queue.customQueueNameBroadcastAllTest";
        String queueName2 = "test.queue2.customQueueNameBroadcastAllTest";
        String exchangeName = "test.exchange.customQueueNameBroadcastAllTest";

        AbstractReceiver<Info> receiver = new AbstractReceiver<Info>() {

        };

        AbstractReceiver<Info> receiver2 = new AbstractReceiver<Info>() {

        };
        subscriber.subscribeTo(Info.class, receiver, queueName, exchangeName);
        subscriber.subscribeTo(Info.class, receiver2, queueName2, exchangeName);
        // When
        publisher.publish(new Info(), exchangeName, Optional.empty());

        Thread.sleep(5_000);
        // Then
        Assert.assertEquals("Check 1 invalid", 1, receiver.getCount().intValue());
        Assert.assertEquals("Check 2 invalid", 1, receiver2.getCount().intValue());
    }

    @Test
    public void onePerMicroserviceTypeTest() {
        // Given
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

        // When : check only one consumer per microservice type receives the event.
        publisher.publish(new OnePerMicroserviceInfo());
        // Then
        Assert.assertFalse(a1.checkCount(1) && a2.checkCount(1));
        Assert.assertTrue(a1.checkCount(1) || a2.checkCount(1));
        Assert.assertFalse(b1.checkCount(1) && b2.checkCount(1));
        Assert.assertTrue(b1.checkCount(1) || b2.checkCount(1));
    }

    public void testErrorMsg() throws InterruptedException {
        // First lets subscribe to a queue
        subscriber.subscribeTo(ErrorEvent.class, new ErrorHandler());
        try {
            rabbitVirtualHostAdmin.bind(AmqpChannel.AMQP_INSTANCE_MANAGER);
            // Purge DLQ before doing anything
            amqpAdmin.purgeQueue(amqpAdmin.getDefaultDLQName(), true);
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
        try {
            rabbitVirtualHostAdmin.bind(AmqpChannel.AMQP_MULTITENANT_MANAGER);
            // sends a malformed message to the queue used by the handler (no tenant wrapper)
            Target target = ErrorEvent.class.getAnnotation(Event.class).target();
            String exchangeName = amqpAdmin.getBroadcastExchangeName(ErrorEvent.class.getName(), target);

            // Publish the wrong message on ErrorEvent queue
            // To do so, we have to do it by hand
            OnePerMicroserviceInfo wrongEvent = new OnePerMicroserviceInfo();
            wrongEvent.setMessage(wrongEvent.getMessage() + Math.random());
            TenantWrapper<OnePerMicroserviceInfo> messageSended = TenantWrapper.build(wrongEvent, "PROJECT");
            rabbitTemplate.convertAndSend(exchangeName,
                                          RegardsAmqpAdmin.DEFAULT_ROUTING_KEY,
                                          messageSended,
                                          pMessage -> {
                                              MessageProperties messageProperties = pMessage.getMessageProperties();
                                              messageProperties.setPriority(0);
                                              return new Message(pMessage.getBody(), messageProperties);
                                          });
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
        try {
            Thread.sleep(1000);
            rabbitVirtualHostAdmin.bind(AmqpChannel.AMQP_INSTANCE_MANAGER);
            // Check that the message ended up in DLQ
            // To do so we have to poll on DLQ one message that is the right one
            Object fromDlq = rabbitTemplate.receiveAndConvert(amqpAdmin.getDefaultDLQName(), 0);
            if (fromDlq == null) {
                Assert.fail("There should be a message into DLQ.");
                return;
            }
            if (fromDlq instanceof TenantWrapper) {
                Object content = ((TenantWrapper<?>) fromDlq).getContent();
                if (!(content instanceof OnePerMicroserviceInfo)) {
                    Assert.fail(String.format("Message from DLQ is not %s but %s",
                                              OnePerMicroserviceInfo.class.getName(),
                                              content.getClass().getName()));
                }
            } else {
                Assert.fail("Message from DLQ is not a TenantWrapper. You might have been compromised by other tests.");
            }
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
    }

    private class MicroserviceReceiver extends AbstractReceiver<MicroserviceInfo> {

    }

    private class Receiver extends AbstractReceiver<Info> {

    }

    private class SingleReceiverA extends AbstractReceiver<OnePerMicroserviceInfo> {

    }

    private class SingleReceiverB extends AbstractReceiver<OnePerMicroserviceInfo> {

    }

    private class ErrorHandler extends AbstractReceiver<ErrorEvent> {

        @Override
        public void handle(String tenant, ErrorEvent message) {
            throw new RuntimeException("Because");
        }
    }

    private class NotificationRequestEventReceiver extends AbstractReceiver<NotificationRequestEvent> {

        private TenantWrapper<NotificationRequestEvent> lastWrapper;

        private NotificationRequestEvent lastInfo;

        @Override
        protected void doHandle(TenantWrapper<NotificationRequestEvent> wrapper) {
            this.lastWrapper = wrapper;
            this.lastInfo = wrapper.getContent();
        }

        public NotificationRequestEvent getLastInfo() {
            return lastInfo;
        }

        public TenantWrapper<NotificationRequestEvent> getLastWrapper() {
            return lastWrapper;
        }
    }
}
