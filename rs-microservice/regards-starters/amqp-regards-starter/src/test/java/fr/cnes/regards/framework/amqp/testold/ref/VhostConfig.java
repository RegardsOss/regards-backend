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
package fr.cnes.regards.framework.amqp.testold.ref;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SimpleRoutingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gary Russell
 *
 */
@Configuration
@EnableTransactionManagement
public class VhostConfig {

    public static final String QUEUE_NAME = "vhostq";

    public static final String VHOST1 = "vhost1";

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        // rabbitTemplate.setRoutingKey(QUEUE_NAME);
        rabbitTemplate.setQueue(QUEUE_NAME);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setChannelTransacted(true);
        return rabbitTemplate;
    }

    @Bean
    public ConnectionFactory connectionFactory() {

        Map<Object, ConnectionFactory> targetConnectionFactories = new HashMap<>();

        CachingConnectionFactory vhostFactory = new CachingConnectionFactory();
        vhostFactory.setAddresses("127.0.0.1:5672");
        vhostFactory.setUsername("guest");
        vhostFactory.setPassword("guest");
        vhostFactory.setVirtualHost(VHOST1);
        targetConnectionFactories.put(VHOST1, vhostFactory);

        SimpleRoutingConnectionFactory connectionFactory = new SimpleRoutingConnectionFactory();
        connectionFactory.setTargetConnectionFactories(targetConnectionFactories);
        return connectionFactory;
    }

    @Bean
    public RabbitTransactionManager transactionManager() {
        return new RabbitTransactionManager(connectionFactory());
    }

    @Bean
    public Service service() {
        return new VhostServiceImpl();
    }
}
