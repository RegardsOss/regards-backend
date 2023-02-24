/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.EventUtils;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;

import java.util.Optional;

/**
 * Parameters used to define an AMQP communication channel.
 * A channel is composed of AMQP queue, exchange and bindings configuration.
 *
 * @author Sébastien Binda
 */
public class AmqpChannel {

    /**
     * Base name for the AMQP manager virtual host.
     */
    public static final String AMQP_INSTANCE_MANAGER = "regards.instance.manager";

    public static final String AMQP_MULTITENANT_MANAGER = "regards.multitenant.manager";

    public static final String INSTANCE_TENANT = "instance";

    /**
     * Event class type to configure binding for.
     */
    private Class<?> eventType;

    /**
     * {@link WorkerMode} to configure binding for.
     * This mode is used to create a queue/exchange/binding associated
     */
    private WorkerMode workerMode;

    /**
     * {@link Target}
     * This target is used to create a queue/exchange/binding associated
     */
    private Target target;

    /**
     * Handler type to listen for events when subscribing.
     */
    private Optional<Class<? extends IHandler<?>>> handlerType = Optional.empty();

    /**
     * If not provided the exchange name is calculated from the eventType, WorkerMode and Target
     */
    private Optional<String> exchangeName = Optional.empty();

    /**
     * If not provided the queue name is calculated from the eventType, WorkerMode and Target
     */
    private Optional<String> queueName = Optional.empty();

    /**
     * Routing key used to bind queue to exchange and to send events when publishing
     */
    private Optional<String> routingKey = Optional.empty();

    /**
     * Indicates if a single DLQ is created for the associated queue. If no use the standard common regards DLQ.
     */
    private boolean isDedicatedDLQEnabled = false;

    /**
     * Does channel needs to declare a dlq on the created queue
     * default: true
     */
    private boolean declareDlq = true;

    /**
     * Indicates the routing key assoiated to the dlq of the current queue.
     */
    private Optional<String> deadLetterQueueRoutingKey = Optional.empty();

    /**
     * If true creates the queue with autoDelete option.
     * Auto delete option enable deletion of queues as soon as no consumer is connected to.
     */
    private boolean autoDeleteQueue = false;

    /**
     * Build AmqpChannel without reading {@link WorkerMode}, {@link Target} and routingKey
     * from {@link fr.cnes.regards.framework.amqp.event.Event} on eventType
     */
    public static AmqpChannel build(Class<?> eventType, WorkerMode workerMode, Target target) {
        AmqpChannel conf = new AmqpChannel();
        conf.eventType = eventType;
        conf.workerMode = workerMode;
        conf.target = target;
        return conf;
    }

    /**
     * Build AmqpChannel by reading {@link WorkerMode}, {@link Target} and routingKey
     * from {@link fr.cnes.regards.framework.amqp.event.Event} on eventType
     */
    public static AmqpChannel build(Class<?> eventType) {
        AmqpChannel conf = new AmqpChannel();
        conf.eventType = eventType;
        conf.workerMode = EventUtils.getWorkerMode(eventType);
        conf.target = EventUtils.getTargetRestriction(eventType);
        conf.routingKey = Optional.ofNullable(EventUtils.getRoutingKey(eventType));
        conf.declareDlq = EventUtils.isDeclareDlq(eventType);
        return conf;
    }

    public AmqpChannel exchange(String exchangeName) {
        this.exchangeName = Optional.ofNullable(exchangeName);
        return this;
    }

    public AmqpChannel queue(String queueName) {
        this.queueName = Optional.ofNullable(queueName);
        return this;
    }

    public AmqpChannel autoDelete() {
        this.autoDeleteQueue = true;
        return this;
    }

    public AmqpChannel declareDlq() {
        this.declareDlq = true;
        return this;
    }

    public AmqpChannel dedicatedDlq() {
        this.isDedicatedDLQEnabled = true;
        return this;
    }

    public AmqpChannel withDlqRoutingKey(String routingKey) {
        this.deadLetterQueueRoutingKey = Optional.ofNullable(routingKey);
        return this;
    }

    public AmqpChannel withRoutingKey(String routingKey) {
        this.routingKey = Optional.ofNullable(routingKey);
        return this;
    }

    public AmqpChannel forHandler(IHandler handler) {
        this.handlerType = Optional.ofNullable(handler.getType());
        if (handler instanceof IBatchHandler) {
            this.isDedicatedDLQEnabled = ((IBatchHandler<?>) handler).isDedicatedDLQEnabled();
            this.deadLetterQueueRoutingKey = (((IBatchHandler<?>) handler).getDLQRoutingKey());
        }
        return this;
    }

    public AmqpChannel forHandlerType(Class<? extends IHandler<?>> handlerType) {
        this.handlerType = Optional.ofNullable(handlerType);
        return this;
    }

    public AmqpChannel autoDelete(boolean autoDeleteQueue) {
        this.autoDeleteQueue = autoDeleteQueue;
        return this;
    }

    public Class<?> getEventType() {
        return eventType;
    }

    public void setEventType(Class<?> eventType) {
        this.eventType = eventType;
    }

    public WorkerMode getWorkerMode() {
        return workerMode;
    }

    public void setWorkerMode(WorkerMode workerMode) {
        this.workerMode = workerMode;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public Optional<Class<? extends IHandler<?>>> getHandlerType() {
        return handlerType;
    }

    public void setHandlerType(Optional<Class<? extends IHandler<?>>> handlerType) {
        this.handlerType = handlerType;
    }

    public Optional<String> getQueueName() {
        return queueName;
    }

    public void setQueueName(Optional<String> queueName) {
        this.queueName = queueName;
    }

    public boolean isDedicatedDLQEnabled() {
        return isDedicatedDLQEnabled;
    }

    public void setDedicatedDLQEnabled(boolean dedicatedDLQEnabled) {
        isDedicatedDLQEnabled = dedicatedDLQEnabled;
    }

    public Optional<String> getDeadLetterQueueRoutingKey() {
        return deadLetterQueueRoutingKey;
    }

    public void setDeadLetterQueueRoutingKey(Optional<String> deadLetterQueueRoutingKey) {
        this.deadLetterQueueRoutingKey = deadLetterQueueRoutingKey;
    }

    public boolean isAutoDeleteQueue() {
        return autoDeleteQueue;
    }

    public void setAutoDeleteQueue(boolean autoDeleteQueue) {
        this.autoDeleteQueue = autoDeleteQueue;
    }

    public Optional<String> getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(Optional<String> exchangeName) {
        this.exchangeName = exchangeName;
    }

    public Optional<String> getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(Optional<String> routingKey) {
        this.routingKey = routingKey;
    }

    public boolean isDeclareDlq() {
        return declareDlq;
    }
}
