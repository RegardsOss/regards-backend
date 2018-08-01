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
package fr.cnes.regards.framework.amqp;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.EventUtils;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;

/**
 *
 * Common publisher methods
 *
 * @author Marc Sordi
 *
 */
public abstract class AbstractPublisher implements IPublisherContract {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPublisher.class);

    /**
     * bean allowing us to send message to the broker
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * configuration initializing required bean
     */
    private final IAmqpAdmin amqpAdmin;

    /**
     * Virtual host admin
     */
    private final IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    public AbstractPublisher(RabbitTemplate pRabbitTemplate, IAmqpAdmin amqpAdmin,
            IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin) {
        this.rabbitTemplate = pRabbitTemplate;
        this.amqpAdmin = amqpAdmin;
        this.rabbitVirtualHostAdmin = pRabbitVirtualHostAdmin;
    }

    @Override
    public void publish(ISubscribable event) {
        publish(event, 0);
    }

    @Override
    public void publish(ISubscribable event, int pPriority) {
        Class<?> eventClass = event.getClass();
        publish(event, EventUtils.getWorkerMode(eventClass), EventUtils.getTargetRestriction(eventClass), pPriority,
                false);
    }

    @Override
    public void publish(IPollable event) {
        publish(event, 0, false);
    }

    @Override
    public void publish(IPollable event, boolean purgeQueue) {
        publish(event, 0, purgeQueue);
    }

    @Override
    public void publish(IPollable event, int priority) {
        publish(event, priority, false);
    }

    @Override
    public void publish(IPollable event, int priority, boolean purgeQueue) {
        Class<?> eventClass = event.getClass();
        publish(event, WorkerMode.UNICAST, EventUtils.getTargetRestriction(eventClass), priority, purgeQueue);
    }

    @Override
    public void purgeQueue(Class<? extends IPollable> eventType) {
        String tenant = resolveTenant();
        String virtualHost = resolveVirtualHost(tenant);

        try {
            rabbitVirtualHostAdmin.bind(virtualHost);
            Queue queue = amqpAdmin.declareQueue(tenant, eventType, WorkerMode.UNICAST,
                                                 EventUtils.getTargetRestriction(eventType), Optional.empty());
            amqpAdmin.purgeQueue(queue.getName(), false);
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
    }

    /**
     * @param <T>
     *            event to be published
     * @param event
     *            the event you want to publish
     * @param priority
     *            priority given to the event
     * @param workerMode
     *            publishing mode
     * @param target
     *            publishing scope
     * @param purgeQueue true to purge queue if already exists. Useful in tests.
     */
    protected <T> void publish(final T event, final WorkerMode workerMode, final Target target, final int priority,
            boolean purgeQueue) {

        LOGGER.debug("Publishing event {} (Target : {}, WorkerMode : {} )", event.getClass(), target, workerMode);

        String tenant = resolveTenant();
        if (tenant != null) {
            publish(tenant, resolveVirtualHost(tenant), event, workerMode, target, priority, purgeQueue);
        } else {
            String errorMessage = String.format("Unable to publish event %s cause no tenant found.", event.getClass());
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * @return current tenant
     */
    protected abstract String resolveTenant();

    /**
     * @param tenant current tenant
     * @return the virtual host on which we have to publish the event according to the tenant
     */
    protected abstract String resolveVirtualHost(String tenant);

    /**
     * @param <T>
     *            event to be published
     * @param tenant
     *            the tenant name
     * @param virtualHost virtual host for current tenant
     * @param event
     *            the event you want to publish
     * @param priority
     *            priority given to the event
     * @param workerMode
     *            publishing mode
     * @param target
     *            publishing scope
     * @param purgeQueue true to purge queue if already exists. Useful in tests.
     */
    protected final <T> void publish(String tenant, String virtualHost, T event, WorkerMode workerMode, Target target,
            int priority, boolean purgeQueue) {

        final Class<?> eventType = event.getClass();

        try {
            // Bind the connection to the right vHost (i.e. tenant to publish the message)
            rabbitVirtualHostAdmin.bind(virtualHost);

            // Declare exchange
            Exchange exchange = amqpAdmin.declareExchange(eventType, workerMode, target);

            if (WorkerMode.UNICAST.equals(workerMode)) {
                // Direct exchange needs a specific queue, a binding between this queue and exchange containing a
                // specific routing key
                Queue queue = amqpAdmin.declareQueue(tenant, eventType, workerMode, target, Optional.empty());
                if (purgeQueue) {
                    amqpAdmin.purgeQueue(queue.getName(), false);
                }
                amqpAdmin.declareBinding(queue, exchange, workerMode);
                publishMessageByTenant(tenant, exchange.getName(),
                                       amqpAdmin.getRoutingKey(Optional.of(queue), workerMode), event, priority);
            } else if (WorkerMode.BROADCAST.equals(workerMode)) {
                // Routing key useless ... always skipped with a fanout exchange
                publishMessageByTenant(tenant, exchange.getName(), amqpAdmin.getRoutingKey(null, workerMode), event,
                                       priority);
            } else {
                String errorMessage = String.format("Unexpected worker mode : %s.", workerMode);
                LOGGER.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
    }

    /**
     * Publish event in tenant virtual host
     *
     * @param <T>
     *            event type
     * @param tenant
     *            tenant
     * @param exchangeName
     *            {@link Exchange} name
     * @param routingKey
     *            routing key (really useful for direct exchange). Use {@link Publisher#DEFAULT_ROUTING_KEY} for fanout.
     * @param event
     *            the event to publish
     * @param priority
     *            the event priority
     */
    private final <T> void publishMessageByTenant(String tenant, String exchangeName, String routingKey, T event,
            int priority) {

        // Message to publish
        final TenantWrapper<T> messageSended = new TenantWrapper<>(event, tenant);

        // routing key is unnecessary for fanout exchanges but is for direct exchanges
        rabbitTemplate.convertAndSend(exchangeName, routingKey, messageSended, pMessage -> {
            final MessageProperties propertiesWithPriority = pMessage.getMessageProperties();
            propertiesWithPriority.setPriority(priority);
            return new Message(pMessage.getBody(), propertiesWithPriority);
        });
    }
}
