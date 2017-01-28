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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * @author Gary Russell
 *
 */
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

    @Ignore
    @Test
    public void test() {
        final AbstractApplicationContext context = new AnnotationConfigApplicationContext(Config.class);

        RabbitTemplate template = context.getBean(RabbitTemplate.class);
        template.convertAndSend("", Config.QUEUE_NAME, "foo");
        Service service = context.getBean(Service.class);
        try {
            service.process(false);
        } catch (Exception e1) {
            System.out.println(e1.getMessage());
            Assert.fail();
        }
        Object o = template.receiveAndConvert(Config.QUEUE_NAME);
        assertNull(o);
        o = template.receiveAndConvert("txTestQ2");
        assertNotNull(o);
        System.out.println("message " + o + " moved from Q1 to Q2");

        template.convertAndSend("", "txTestQ1", "bar");
        try {
            service.process(true);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        o = template.receiveAndConvert("txTestQ1");
        assertNotNull(o);
        System.out.println("message " + o + " still in Q1");
        o = template.receiveAndConvert("txTestQ2");
        assertNull(o);
    }
}
