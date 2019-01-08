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
package fr.cnes.regards.framework.amqp.configuration;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;

/**
 * This class manage tenant AMQP administration. Each tenant is hosted in an AMQP virtual host.<br/>
 * @author svissier
 * @author Marc Sordi
 */
public class RegardsAmqpAdmin implements IAmqpAdmin {

    private static final String DOT = ".";

    public static final String REGARDS_NAMESPACE = "regards";

    public static final String UNICAST_BASE_EXCHANGE_NAME = "unicast";

    public static final String BROADCAST_BASE_EXCHANGE_NAME = "broadcast";

    public static final String UNICAST_NAMESPACE = REGARDS_NAMESPACE + DOT + UNICAST_BASE_EXCHANGE_NAME;

    public static final String BROADCAST_NAMESPACE = REGARDS_NAMESPACE + DOT + BROADCAST_BASE_EXCHANGE_NAME;

    /**
     * Default routing key
     */
    private static final String DEFAULT_ROUTING_KEY = "";

    /**
     * This constant allows to defined a message to send with a high priority
     */
    public static final Integer MAX_PRIORITY = 255;

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(RegardsAmqpAdmin.class);

    /**
     * Bean allowing us to declare queue, exchange, binding
     */
    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * Microservice type identifier. If not specified, set to spring application name.
     */
    private String microserviceTypeId;

    /**
     * Microservice instance identifier. If not specified, set to spring application name plus a generated unique suffix.
     * Moreover, related queues will be created with <b>auto delete</b> capabilities.
     */
    private String microserviceInstanceId;

    /**
     * Whether above instance identifier is generated or not. If so, queues using instance identifier will be auto delete queues.
     */
    private boolean instanceIdGenerated = false;

    public RegardsAmqpAdmin(String microserviceTypeId, String microserviceInstanceId) {
        this.microserviceTypeId = microserviceTypeId;
        this.microserviceInstanceId = microserviceInstanceId;
    }

    @PostConstruct
    public void init() {
        // Fallback if no microservice instance properties was given
        if (microserviceTypeId == null) {
            microserviceTypeId = microserviceName;
        }
        if (microserviceInstanceId == null) {
            this.instanceIdGenerated = true;
            this.microserviceInstanceId = microserviceName + UUID.randomUUID();
        }
    }

    @Override
    public Exchange declareExchange(Class<?> eventType, WorkerMode workerMode, Target target) {

        Exchange exchange;
        switch (workerMode) {
            case UNICAST:
                exchange = ExchangeBuilder.directExchange(getUnicastExchangeName()).durable(true).build();
                break;
            case BROADCAST:
                exchange = ExchangeBuilder.fanoutExchange(getBroadcastExchangeName(eventType.getName(), target))
                        .durable(true).build();
                break;
            default:
                throw new EnumConstantNotPresentException(WorkerMode.class, workerMode.name());
        }

        rabbitAdmin.declareExchange(exchange);
        return exchange;
    }

    /**
     * Unicast exchange name
     * @return exchange name
     */
    private String getUnicastExchangeName() {
        return UNICAST_NAMESPACE;
    }

    /**
     * Broadcast exchange name by event
     * @return exchange name
     */
    private String getBroadcastExchangeName(String eventType, Target target) {
        StringBuilder builder = new StringBuilder();
        builder.append(BROADCAST_NAMESPACE);
        if (Target.MICROSERVICE.equals(target)) {
            // Restrict exchange to microservice type
            builder.append(DOT);
            builder.append(microserviceTypeId);
        }
        builder.append(DOT);
        builder.append(eventType);
        return builder.toString();
    }

    @Override
    public Queue declareQueue(String tenant, Class<?> eventType, WorkerMode workerMode, Target target,
            Optional<Class<? extends IHandler<?>>> handlerType) {

        Map<String, Object> args = new ConcurrentHashMap<>();
        args.put("x-max-priority", MAX_PRIORITY);

        Queue queue;
        switch (workerMode) {
            case UNICAST:
                // Useful for publishing unicast event and subscribe to a unicast exchange
                queue = QueueBuilder.durable(getUnicastQueueName(tenant, eventType, target)).withArguments(args)
                        .build();
                break;
            case BROADCAST:
                // Allows to subscribe to a broadcast exchange
                if (handlerType.isPresent()) {
                    QueueBuilder qb = QueueBuilder.durable(getSubscriptionQueueName(handlerType.get(), target))
                            .withArguments(args);
                    // FIXME test does not work with auto deletion queues
                    // queue = isAutoDeleteSubscriptionQueue(target) ? qb.autoDelete().build() : qb.build();
                    queue = qb.build();
                } else {
                    throw new IllegalArgumentException("Missing event handler for broadcasted event");
                }
                break;
            default:
                throw new EnumConstantNotPresentException(WorkerMode.class, workerMode.name());
        }

        rabbitAdmin.declareQueue(queue);
        return queue;
    }

    /**
     * Unicast publish queue name build for {@link IPollable} or {@link ISubscribable} event according to its
     * {@link Target} restriction.<br/>
     * Tenant is used for working queues naming to prevent starvation of a project. Otherwise, some projects may
     * monopolize a single multitenant queue!
     * @param tenant tenant (useful for queue naming in single virtual host and compatible with multiple virtual hosts)
     * @param eventType event type
     * @param target {@link Target}
     * @return queue name
     */
    @Override
    public String getUnicastQueueName(String tenant, Class<?> eventType, Target target) {
        if (Target.ONE_PER_MICROSERVICE_TYPE.equals(target)) {
            throw new IllegalArgumentException(String.format("Target %s not supported", target.toString()));
        }

        StringBuilder builder = new StringBuilder();
        builder.append(UNICAST_NAMESPACE);
        builder.append(DOT);
        builder.append(tenant);
        if (Target.MICROSERVICE.equals(target)) {
            builder.append(DOT);
            builder.append(microserviceTypeId);
        }
        builder.append(DOT);
        builder.append(eventType.getName());
        return builder.toString();
    }

    /**
     * Subscription queue name based on event {@link IHandler} type name and {@link Target} restriction. The uniqueness
     * of the queue is guaranteed by the combination of microservice type, id and handler for {@link Target#ALL}
     * type.
     * @param handlerType event handler
     * @return queue name
     */
    @Override
    public String getSubscriptionQueueName(Class<? extends IHandler<?>> handlerType, Target target) {
        StringBuilder builder = new StringBuilder();
        builder.append(BROADCAST_NAMESPACE);
        builder.append(DOT);
        builder.append(microserviceTypeId);
        if (Target.ALL.equals(target) || Target.MICROSERVICE.equals(target)) {
            builder.append(DOT);
            builder.append(microserviceInstanceId);
        }
        builder.append(DOT);
        builder.append(handlerType.getName());
        return builder.toString();
    }

    public boolean isAutoDeleteSubscriptionQueue(Target target) {
        return this.instanceIdGenerated && (Target.ALL.equals(target) || Target.MICROSERVICE.equals(target));
    }

    @Override
    public Binding declareBinding(Queue queue, Exchange exchange, WorkerMode workerMode) {

        Binding binding;
        switch (workerMode) {
            case UNICAST:
                binding = BindingBuilder.bind(queue).to((DirectExchange) exchange)
                        .with(getRoutingKey(Optional.of(queue), WorkerMode.UNICAST));
                break;
            case BROADCAST:
                binding = BindingBuilder.bind(queue).to((FanoutExchange) exchange);
                break;
            default:
                throw new EnumConstantNotPresentException(WorkerMode.class, workerMode.name());
        }

        rabbitAdmin.declareBinding(binding);
        return binding;
    }

    @Override
    public String getRoutingKey(Optional<Queue> queue, WorkerMode workerMode) {
        final String routingKey;
        switch (workerMode) {
            case UNICAST:
                if (queue.isPresent()) {
                    routingKey = queue.get().getName();
                } else {
                    throw new IllegalArgumentException("Queue is required");
                }
                break;
            case BROADCAST:
                routingKey = DEFAULT_ROUTING_KEY;
                break;
            default:
                throw new EnumConstantNotPresentException(WorkerMode.class, workerMode.name());
        }
        return routingKey;
    }

    @Override
    public void purgeQueue(String queueName, boolean noWait) {
        rabbitAdmin.purgeQueue(queueName, noWait);
    }

    @Override
    public Properties getQueueProperties(String queueName) {
        return rabbitAdmin.getQueueProperties(queueName);
    }

    @Override
    public boolean deleteQueue(String queueName) {
        return rabbitAdmin.deleteQueue(queueName);
    }

    @Override
    public void deleteQueue(String queueName, boolean unused, boolean empty) {
        rabbitAdmin.deleteQueue(queueName, unused, empty);
    }

    /**
     * Only used for test purpose
     */
    public void setMicroserviceTypeId(String microserviceTypeId) {
        this.microserviceTypeId = microserviceTypeId;
    }

    /**
     * Only used for test purpose
     */
    public void setMicroserviceInstanceId(String microserviceInstanceId) {
        this.microserviceInstanceId = microserviceInstanceId;
    }
}
