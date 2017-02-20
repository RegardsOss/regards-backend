/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.regards.framework.amqp.test.ref;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * @author Gary Russell
 * @author Marc Sordi
 *
 */
@Ignore("This test context has to be set manually : queues, vhosts, ...")
public class TxTest {

    @Test
    public void simpleTest() {
        final AbstractApplicationContext context = new AnnotationConfigApplicationContext(Config.class);

        RabbitTemplate template = context.getBean(RabbitTemplate.class);

        // Publishing event
        template.convertAndSend("", Config.QUEUE_NAME, "foo");
        Service service = context.getBean(Service.class);
        try {
            // Polling event
            service.process(false);
        } catch (Exception e1) {
            System.out.println(e1.getMessage());
            Assert.fail();
        }
        // Trying to re-poll event
        Object o = template.receiveAndConvert(Config.QUEUE_NAME);
        assertNull(o);

        // Publishing another event
        template.convertAndSend("", Config.QUEUE_NAME, "bar");
        try {
            // Polling event with exception
            service.process(true);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        // Trying to re-poll event
        o = template.receiveAndConvert(Config.QUEUE_NAME);
        assertNotNull(o);
    }

    @Test
    public void multiVhostTest() {
        final AbstractApplicationContext context = new AnnotationConfigApplicationContext(VhostConfig.class);

        RabbitTemplate template = context.getBean(RabbitTemplate.class);

        // Publishing event
        SimpleResourceHolder.bind(template.getConnectionFactory(), VhostConfig.VHOST1);
        template.convertAndSend("", VhostConfig.QUEUE_NAME, "foo");
        SimpleResourceHolder.unbind(template.getConnectionFactory());

        Service service = context.getBean(Service.class);
        try {
            SimpleResourceHolder.bind(template.getConnectionFactory(), VhostConfig.VHOST1);
            // Polling event
            service.process(false);
        } catch (Exception e1) {
            System.out.println(e1.getMessage());
            Assert.fail();
        } finally {
            SimpleResourceHolder.unbind(template.getConnectionFactory());
        }
        // Trying to re-poll event
        SimpleResourceHolder.bind(template.getConnectionFactory(), VhostConfig.VHOST1);
        Object o = template.receiveAndConvert(VhostConfig.QUEUE_NAME);
        SimpleResourceHolder.unbind(template.getConnectionFactory());
        assertNull(o);

        // Publishing another event
        SimpleResourceHolder.bind(template.getConnectionFactory(), VhostConfig.VHOST1);
        template.convertAndSend("", VhostConfig.QUEUE_NAME, "bar");
        SimpleResourceHolder.unbind(template.getConnectionFactory());
        try {
            SimpleResourceHolder.bind(template.getConnectionFactory(), VhostConfig.VHOST1);
            // Polling event with exception
            service.process(true);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            SimpleResourceHolder.unbind(template.getConnectionFactory());
        }
        // Trying to re-poll event
        SimpleResourceHolder.bind(template.getConnectionFactory(), VhostConfig.VHOST1);
        o = template.receiveAndConvert(VhostConfig.QUEUE_NAME);
        SimpleResourceHolder.unbind(template.getConnectionFactory());
        assertNotNull(o);
    }
}
