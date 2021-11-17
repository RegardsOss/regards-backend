/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;

import java.util.Optional;
import java.util.Properties;

/**
 * @author Marc Sordi
 */
public interface IAmqpAdmin {

    /**
     * Declare an exchange.
     * If exchangeName is provided, the declared exchange is named with it or else the exchange name is computed with :
     * <ul>
     *     <li>{@link WorkerMode#UNICAST} : creates a {@link org.springframework.amqp.core.DirectExchange} with {@link IAmqpAdmin#getUnicastExchangeName}</li>
     *     <li>{@link WorkerMode#BROADCAST} : creates a {@link org.springframework.amqp.core.FanoutExchange} with {@link IAmqpAdmin#getBroadcastExchangeName}</li>
     * </ul>
     * @param channel Channel configuration for exchange/queue/binding
     * @return a new {@link Exchange} related to current tenant and event
     */
    Exchange declareExchange(AmqpChannel channel);

    /**
     * Broadcast exchange name by event
     * <ul>
     *   <li>{@link Target#MICROSERVICE} : regards.broadcast.microserviceType.EventType</li>
     *   <li>{@link Target#ALL}/{@link Target#ONE_PER_MICROSERVICE_TYPE} : regards.broadcast..EventType</li>
     * </ul>
     * @return exchange name
     */
    String getBroadcastExchangeName(String eventType, Target target);

    /**
     * Return common unicast exchange name : regards.unicast
     * @return
     */
    String getUnicastExchangeName();

    /**
     * Declare dead letter exchange and queue
     */
    void declareDeadLetter();

    /**
     * Declare a queue that can handle priority with custom Dead Letter Exchange and custom Dead Letter Routing Key
     * If queueName is provided the declared queue is named with it or else the queue name is computed with :
     * <ul>
     *     <li>{@link WorkerMode#UNICAST} : {@link IAmqpAdmin#getUnicastQueueName}</li>
     *     <li>{@link WorkerMode#BROADCAST} : {@link IAmqpAdmin#getSubscriptionQueueName}</li>
     * </ul>
     * @param tenant tenant for which the queue is created
     * @param channel Channel configuration for exchange/queue/binding
     * @return declared {@link Queue}
     */
    Queue declareQueue(String tenant, AmqpChannel channel);

    /**
     * Compute {@link WorkerMode#UNICAST} queue name with :
     * <ul>
     *  <li>{@link Target#MICROSERVICE} : regards.unicast.tenant.microserviceType.EventType</li>
     *  <li>{@link Target#ALL}/{@link Target#MICROSERVICE} : regards.unicast.tenant.EventType</li>
     *  <li>{@link Target#ONE_PER_MICROSERVICE_TYPE}/{@link Target#MICROSERVICE} : {@link IllegalArgumentException}</li>
     * </ul>
     * @param tenant
     * @param eventType
     * @param target
     * @return
     */
    String getUnicastQueueName(String tenant, Class<?> eventType, Target target);

    /**
     * Compute {@link WorkerMode#BROADCAST} queue name with :
     * <ul>
     *   <li>{@link Target#ONE_PER_MICROSERVICE_TYPE} : regards.broadcast.microserviceType.handlerType</li>
     *   <li>{@link Target#ALL}/{@link Target#MICROSERVICE} : regards.broadcast.microserviceType.microserviceId.handlerType</li>
     * </ul>
     * @param handlerType
     * @param target
     * @return
     */
    String getSubscriptionQueueName(Class<? extends IHandler<?>> handlerType, Target target);

    String getDedicatedDLQFromQueueName(String queueName);

    String getDedicatedDLRKFromQueueName(String queueName);

    /**
     * Declare binding to link {@link Queue} and {@link Exchange} with an optional routing key
     * @param queue {@link Queue} to bind
     * @param exchange {@link Exchange} to bind
     * @param workerMode {@link WorkerMode} to compute routing key
     * @param broadcastRoutingKey optional routing key in case of broadcast. Default is {@link RegardsAmqpAdmin#DEFAULT_ROUTING_KEY}.
     * @return {@link Binding}
     */
    Binding declareBinding(Queue queue, Exchange exchange, WorkerMode workerMode, Optional<String> broadcastRoutingKey);

    /**
     * Routing key build according to {@link WorkerMode}.
     * @param queue queue
     * @param workerMode worker mode
     * @param broadcastRoutingKey optional routing key in case of broadcast. Default is {@link RegardsAmqpAdmin#DEFAULT_ROUTING_KEY}.
     * @return routing key
     */
    String getRoutingKey(Optional<Queue> queue, WorkerMode workerMode, Optional<String> broadcastRoutingKey);

    /**
     * Purge the queue that manages the specified event
     * @param noWait true to not await completion of the purge
     */
    void purgeQueue(String queueName, boolean noWait);

    /**
     * Check if a queue has messages on it
     * @param queueName the name of the queue
     * @return true when the queue is empty
     */
    boolean isQueueEmpty(String queueName);

    /**
     * Get queue properties
     * @param queueName queue name
     * @return properties
     */
    Properties getQueueProperties(String queueName);

    /**
     * @param queueName queue name
     * @return true if the queued existed and was deleted
     */
    boolean deleteQueue(String queueName);

    /**
     * @param queueName queue name
     * @param unused true if the queue should be deleted only if not in use
     * @param empty true if the queue should be deleted only if not in use
     */
    void deleteQueue(String queueName, boolean unused, boolean empty);

    /**
     * @return default dead letter exchange name
     */
    String getDefaultDLXName();

    /**
     * @return default dead letter queue name
     */
    String getDefaultDLQName();
}
