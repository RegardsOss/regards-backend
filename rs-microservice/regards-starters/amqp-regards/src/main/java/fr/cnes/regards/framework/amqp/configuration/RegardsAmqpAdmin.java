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
package fr.cnes.regards.framework.amqp.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;

/**
 * This class manage tenant AMQP administration. Each tenant is hosted in an AMQP virtual host.<br/>
 *
 * @author svissier
 * @author Marc Sordi
 *
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

    /**
     * Microservice type identifier
     */
    private final String microserviceTypeId;

    /**
     * Microservice instance identifier
     */
    private final String microserviceInstanceId;

    public RegardsAmqpAdmin(String microserviceTypeId, String microserviceInstanceId) {
        this.microserviceTypeId = microserviceTypeId;
        this.microserviceInstanceId = microserviceInstanceId;
    }

    @Override
    public Exchange declareExchange(Class<?> eventType, WorkerMode workerMode, Target target) {

        Exchange exchange;
        switch (workerMode) {
            case UNICAST:
                exchange = new DirectExchange(getUnicastExchangeName(target), true, false);
                break;
            case BROADCAST:
                exchange = new FanoutExchange(getBroadcastExchangeName(eventType.getName(), target), true, false);
                break;
            default:
                throw new EnumConstantNotPresentException(WorkerMode.class, workerMode.name());
        }

        rabbitAdmin.declareExchange(exchange);
        return exchange;
    }

    /**
     * Unicast exchange name build according to event {@link Target} restriction.
     *
     * @param target {@link Target}
     * @return exchange name
     */
    private String getUnicastExchangeName(Target target) {
        StringBuilder builder = new StringBuilder();
        builder.append(UNICAST_NAMESPACE);
        builder.append(manageExchangeTargetRestriction(target));
        return builder.toString();
    }

    /**
     * Broadcast exchange name build by event according to its {@link Target} restriction.
     *
     * @param target {@link Target}
     * @return exchange name
     */
    private String getBroadcastExchangeName(String eventName, Target target) {
        StringBuilder builder = new StringBuilder();
        builder.append(BROADCAST_NAMESPACE);
        builder.append(manageExchangeTargetRestriction(target));
        builder.append(DOT);
        builder.append(eventName);
        return builder.toString();
    }

    /**
     * Build {@link Target} event restriction namespace
     * @param target
     * @return
     */
    private String manageExchangeTargetRestriction(Target target) {
        StringBuilder builder = new StringBuilder();
        switch (target) {
            case ALL:
                // No prefix cause no target restriction
                break;
            case MICROSERVICE:
                builder.append(DOT);
                builder.append(microserviceTypeId);
                break;
            case INSTANCE:
                builder.append(DOT);
                builder.append(microserviceTypeId);
                builder.append(DOT);
                builder.append(microserviceInstanceId);
                break;
            default:
                throw new EnumConstantNotPresentException(Target.class, target.name());
        }
        return builder.toString();
    }

    @Override
    public Queue declareQueue(String tenant, Class<?> eventType, WorkerMode workerMode, Target target,
            Optional<Class<? extends IHandler<?>>> handlerType) {

        Map<String, Object> args = new HashMap<>();
        args.put("x-max-priority", MAX_PRIORITY);

        Queue queue;
        switch (workerMode) {
            case UNICAST:
                // Useful for publishing unicast event and subscribe to a unicast exchange
                queue = new Queue(getUnicastQueueName(tenant, eventType, target), true, false, false, args);
                break;
            case BROADCAST:
                // Allows to subscribe to a broadcast exchange
                if (handlerType.isPresent()) {
                    queue = new Queue(getSubscriptionQueueName(handlerType.get()), true, false, false, args);
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
     * Unicast publish queue name build by {@link IPollable} or {@link ISubscribable} event according to its
     * {@link Target} restriction.<br/>
     * Tenant is used for working queues naming to prevent starvation of a project. Otherwise, some projects may
     * monopolize a single multitenant queue!
     *
     * @param tenant tenant (useful for queue naming in single virtual host and compatible with multiple virtual hosts)
     * @param eventType event type
     * @param target {@link Target}
     * @return queue name
     */
    private String getUnicastQueueName(String tenant, Class<?> eventType, Target target) {
        StringBuilder builder = new StringBuilder();
        builder.append(UNICAST_NAMESPACE);
        builder.append(DOT);
        builder.append(tenant);
        builder.append(manageExchangeTargetRestriction(target));
        builder.append(DOT);
        builder.append(eventType.getName());
        return builder.toString();
    }

    /**
     * Subscription queue name based on event {@link IHandler} type name. {@link Target} restriction is manage at
     * exchange level. The uniqueness of the queue is guaranteed by the combination of microservice type, id and handler
     * type.
     * @param handlerType event handler
     * @return queue name
     */
    private String getSubscriptionQueueName(Class<? extends IHandler<?>> handlerType) {
        StringBuilder builder = new StringBuilder();
        builder.append(BROADCAST_NAMESPACE);
        builder.append(DOT);
        builder.append(microserviceTypeId);
        builder.append(DOT);
        builder.append(microserviceInstanceId);
        builder.append(DOT);
        builder.append(handlerType.getName());
        return builder.toString();
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
}
