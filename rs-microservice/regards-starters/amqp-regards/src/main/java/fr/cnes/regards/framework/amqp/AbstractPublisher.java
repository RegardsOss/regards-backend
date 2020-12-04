/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.event.EventUtils;
import fr.cnes.regards.framework.amqp.event.IMessagePropertiesAware;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;

/**
 * Common publisher methods
 * @author Marc Sordi
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
     * Bean allowing us to declare queue, exchange, binding
     */
    private final RabbitAdmin rabbitAdmin;

    /**
     * configuration initializing required bean
     */
    private final IAmqpAdmin amqpAdmin;

    /**
     * Virtual host admin
     */
    private final IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    /**
     * Map tracing already published events to avoid redeclaring all AMQP elements
     */
    private final Map<String, Boolean> alreadyPublished = new HashMap<>();

    private final Map<String, String> exchangesByEvent = new HashMap<>();

    private final Map<String, String> routingKeysByEvent = new HashMap<>();

    public AbstractPublisher(RabbitTemplate rabbitTemplate, RabbitAdmin rabbitAdmin, IAmqpAdmin amqpAdmin,
            IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitAdmin = rabbitAdmin;
        this.amqpAdmin = amqpAdmin;
        this.rabbitVirtualHostAdmin = pRabbitVirtualHostAdmin;
    }

    @Override
    public void health(Builder builder) {
        LOGGER.debug("Multitenant health check");

        String tenant = resolveTenant();
        if (tenant == null) {
            builder.down().withDetail("tenant", "Unknown tenant").build();
            return;
        }

        try {
            // Bind the connection to the right vhost
            rabbitVirtualHostAdmin.bind(resolveVirtualHost(tenant));

            String version = rabbitTemplate
                    .execute((channel) -> channel.getConnection().getServerProperties().get("version").toString());
            builder.up().withDetail("version", version).build();
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
    }

    @Override
    public void publish(ISubscribable event) {
        publish(event, 0);
    }

    @Override
    @Transactional
    public void publish(List<? extends ISubscribable> events) {
        events.forEach(e -> publish(e));
    }

    @Override
    public void publish(ISubscribable event, int pPriority) {
        Class<?> eventClass = event.getClass();
        publish(event, EventUtils.getWorkerMode(eventClass), EventUtils.getTargetRestriction(eventClass), pPriority,
                false);
    }

    @Override
    @Transactional
    public void publish(List<? extends ISubscribable> events, int priority) {
        events.forEach(e -> publish(e, priority));
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

    @Override
    public void broadcast(String exchangeName, Optional<String> queueName, int priority, Object message,
            Map<String, Object> headers) {

        LOGGER.debug("Broadcasting object {} to exchange {} with priority {}. Binded queue : {}.", message.getClass(),
                     exchangeName, priority, queueName.orElse("None"));

        String tenant = resolveTenant();
        if (tenant == null) {
            String errorMessage = String.format("Unable to publish event %s cause no tenant found.",
                                                message.getClass());
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        try {
            // Bind the connection to the right vhost
            rabbitVirtualHostAdmin.bind(resolveVirtualHost(tenant));

            // Declare AMQP elements

            // Declare exchange
            Exchange exchange = ExchangeBuilder.fanoutExchange(exchangeName).durable(true).build();
            rabbitAdmin.declareExchange(exchange);

            // Queue
            if (queueName.isPresent()) {
                Map<String, Object> args = new ConcurrentHashMap<>();
                args.put("x-max-priority", RegardsAmqpAdmin.MAX_PRIORITY);
                args.put("x-dead-letter-exchange", RegardsAmqpAdmin.REGARDS_DLX);
                args.put("x-dead-letter-routing-key", RegardsAmqpAdmin.REGARDS_DLQ);
                Queue queue = QueueBuilder.durable(queueName.get()).withArguments(args).build();
                rabbitAdmin.declareQueue(queue);

                // Bind queue
                Binding binding = BindingBuilder.bind(queue).to((FanoutExchange) exchange);
                rabbitAdmin.declareBinding(binding);
            }

            // Send message
            publishMessageByTenant(tenant, exchangeName, RegardsAmqpAdmin.DEFAULT_ROUTING_KEY, message, priority,
                                   headers);
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
    }

    @Override
    @Transactional
    public void broadcastAll(String exchangeName, Optional<String> queueName, int priority, Collection<?> messages,
            Map<String, Object> headers) {
        messages.forEach(message -> broadcast(exchangeName, queueName, priority, message, headers));
    }

    /**
     * @param <T> event to be published
     * @param event the event you want to publish
     * @param priority priority given to the event
     * @param workerMode publishing mode
     * @param target publishing scope
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
     * @param <T> event to be published
     * @param tenant the tenant name
     * @param virtualHost virtual host for current tenant
     * @param event the event you want to publish
     * @param priority priority given to the event
     * @param workerMode publishing mode
     * @param target publishing scope
     * @param purgeQueue true to purge queue if already exists. Useful in tests.
     */
    protected final <T> void publish(String tenant, String virtualHost, T event, WorkerMode workerMode, Target target,
                                     int priority, boolean purgeQueue) {

        final Class<?> eventType = event.getClass();

        try {
            // Bind the connection to the right vHost (i.e. tenant to publish the message)
            rabbitVirtualHostAdmin.bind(virtualHost);

            // Declare AMQP elements for first publication
            if (!alreadyPublished.containsKey(eventType.getName())) {
                amqpAdmin.declareDeadLetter();

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
                    cacheAmqpElements(eventType, exchange.getName(),
                                      amqpAdmin.getRoutingKey(Optional.of(queue), workerMode));
                } else if (WorkerMode.BROADCAST.equals(workerMode)) {
                    // Routing key useless ... always skipped with a fanout exchange
                    cacheAmqpElements(eventType, exchange.getName(),
                                      amqpAdmin.getRoutingKey(Optional.empty(), workerMode));
                } else {
                    String errorMessage = String.format("Unexpected worker mode : %s.", workerMode);
                    LOGGER.error(errorMessage);
                    throw new IllegalArgumentException(errorMessage);
                }
                alreadyPublished.put(eventType.getName(), Boolean.TRUE);
            }

            // Publish
            publishMessageByTenant(tenant, exchangesByEvent.get(eventType.getName()),
                                   routingKeysByEvent.get(eventType.getName()), event, priority, null);
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
    }

    private <T> void cacheAmqpElements(Class<?> eventType, String exchangeName, String routingKey) {
        exchangesByEvent.put(eventType.getName(), exchangeName);
        routingKeysByEvent.put(eventType.getName(), routingKey);
    }

    /**
     * Publish event in tenant virtual
     * @param <T> event type
     * @param tenant tenant
     * @param exchangeName {@link Exchange} name
     * @param routingKey routing key (really useful for direct exchange).
     * @param event the event to publish
     * @param priority the event priority
     * @param headers additional headers
     */
    private final <T> void publishMessageByTenant(String tenant, String exchangeName, String routingKey, T event,
            int priority, Map<String, Object> headers) {

        // routing key is unnecessary for fanout exchanges but is for direct exchanges
        rabbitTemplate.convertAndSend(exchangeName, routingKey, event, message -> {
            MessageProperties messageProperties = message.getMessageProperties();
            // Add headers from parameter
            if (headers != null) {
                headers.forEach((k, v) -> messageProperties.setHeader(k, v));
            }
            // Add headers from event
            if (IMessagePropertiesAware.class.isAssignableFrom(event.getClass())) {
                MessageProperties mp = ((IMessagePropertiesAware) event).getMessageProperties();
                if (mp != null) {
                    mp.getHeaders().forEach((k, v) -> messageProperties.setHeader(k, v));
                }
            }
            messageProperties.setHeader(AmqpConstants.REGARDS_TENANT_HEADER, tenant);
            messageProperties.setPriority(priority);
            return new Message(message.getBody(), messageProperties);
        });
    }
}
