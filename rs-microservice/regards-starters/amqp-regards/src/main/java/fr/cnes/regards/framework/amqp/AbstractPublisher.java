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
package fr.cnes.regards.framework.amqp;

import fr.cnes.regards.framework.amqp.configuration.*;
import fr.cnes.regards.framework.amqp.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Common publisher methods
 *
 * @author Marc Sordi
 */
public abstract class AbstractPublisher implements IPublisherContract {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPublisher.class);

    private static final String NO_TENANT_MESSAGE_FORMAT = "Unable to publish event %s because no tenant is being given.";

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
     * Map tracing already published events to avoid redeclaring all AMQP elements.
     * Routing key can be different from one tenant to another, so for simplicity we decided to declare so declare thing once per tenant.
     */
    private final ConcurrentMap<String, ConcurrentMap<String, ExchangeAndRoutingKey>> exchangesAndRoutingKeysByEventPerTenant = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, ConcurrentMap<String, Boolean>> broadcastExchangesPerTenant = new ConcurrentHashMap<>();

    public AbstractPublisher(RabbitTemplate rabbitTemplate,
                             RabbitAdmin rabbitAdmin,
                             IAmqpAdmin amqpAdmin,
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

            String version = rabbitTemplate.execute((channel) -> channel.getConnection()
                                                                        .getServerProperties()
                                                                        .get("version")
                                                                        .toString());
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
    @Transactional
    public void publish(List<? extends ISubscribable> events, String exchangeName, Optional<String> queueName) {
        events.forEach(e -> publish(e, exchangeName, queueName));
    }

    @Override
    public void publish(ISubscribable event, int priority) {
        Class<? extends ISubscribable> eventClass = event.getClass();
        AmqpChannel channel = AmqpChannel.build(eventClass);
        publish(event, priority, channel, false);
    }

    @Override
    public void publish(ISubscribable event, String exchangeName, Optional<String> queueName) {
        Class<? extends ISubscribable> eventClass = event.getClass();
        AmqpChannel channel = AmqpChannel.build(eventClass).exchange(exchangeName);
        queueName.ifPresent(qn -> channel.queue(qn));
        publish(event, 0, channel, false);
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
        String tenant = resolveTenant();
        if (tenant != null) {
            publish(event,
                    priority,
                    AmqpChannel.build(eventClass, WorkerMode.UNICAST, EventUtils.getTargetRestriction(eventClass)),
                    purgeQueue);
        } else {
            String errorMessage = String.format("Unable to publish event %s cause no tenant found.", event.getClass());
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    @Override
    public void purgeQueue(Class<? extends IPollable> eventType) {
        String tenant = resolveTenant();
        String virtualHost = resolveVirtualHost(tenant);

        try {
            rabbitVirtualHostAdmin.bind(virtualHost);
            Queue queue = amqpAdmin.declareQueue(tenant,
                                                 AmqpChannel.build(eventType,
                                                                   WorkerMode.UNICAST,
                                                                   EventUtils.getTargetRestriction(eventType)));
            amqpAdmin.purgeQueue(queue.getName(), false);
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
    }

    @Override
    public void broadcast(String exchangeName,
                          Optional<String> queueName,
                          Optional<String> routingKey,
                          Optional<String> dlk,
                          int priority,
                          Object message,
                          Map<String, Object> headers) {

        LOGGER.debug("Broadcasting object {} to exchange {} with priority {}. Binded queue : {}.",
                     message.getClass(),
                     exchangeName,
                     priority,
                     queueName.orElse("None"));

        String currentTenant = resolveTenant();
        if (currentTenant == null) {
            String errorMessage = String.format(NO_TENANT_MESSAGE_FORMAT, message.getClass(), currentTenant);
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        try {
            // Bind the connection to the right vhost
            rabbitVirtualHostAdmin.bind(resolveVirtualHost(currentTenant));

            ConcurrentMap<String, Boolean> exchanges = broadcastExchangesPerTenant.computeIfAbsent(currentTenant,
                                                                                                   newTenant -> new ConcurrentHashMap<>());
            exchanges.computeIfAbsent(exchangeName, newExchangeName -> {
                // Declare AMQP elements

                // Declare exchange
                Exchange exchange;
                if (routingKey.isPresent()) {
                    exchange = ExchangeBuilder.topicExchange(newExchangeName).durable(true).build();
                } else {
                    exchange = ExchangeBuilder.fanoutExchange(newExchangeName).durable(true).build();
                }
                rabbitAdmin.declareExchange(exchange);

                // Queue
                if (queueName.isPresent()) {
                    Queue queue = QueueBuilder.durable(queueName.get())
                                              .maxPriority(RegardsAmqpAdmin.MAX_PRIORITY)
                                              .deadLetterExchange(amqpAdmin.getDefaultDLXName())
                                              .deadLetterRoutingKey(dlk.orElse(amqpAdmin.getDefaultDLQName()))
                                              .build();
                    rabbitAdmin.declareQueue(queue);

                    // Bind queue
                    Binding binding;
                    if (routingKey.isPresent()) {
                        binding = BindingBuilder.bind(queue).to((TopicExchange) exchange).with(routingKey.get());
                    } else {
                        binding = BindingBuilder.bind(queue).to((FanoutExchange) exchange);
                    }
                    rabbitAdmin.declareBinding(binding);
                }
                return Boolean.TRUE;
            });

            // Send message
            publishMessageByTenant(currentTenant,
                                   exchangeName,
                                   RegardsAmqpAdmin.DEFAULT_ROUTING_KEY,
                                   message,
                                   priority,
                                   headers);
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
    }

    @Override
    public void basicPublish(String tenant, String exchange, String routingKey, Message message) {

        if (tenant == null) {
            String errorMessage = String.format(NO_TENANT_MESSAGE_FORMAT, message.getClass());
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        try {
            // Bind the connection to the right vhost
            rabbitVirtualHostAdmin.bind(resolveVirtualHost(tenant));
            // Send the message
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
    }

    @Override
    @Transactional
    public void broadcastAll(String exchangeName,
                             Optional<String> queueName,
                             Optional<String> routingKey,
                             Optional<String> dlk,
                             int priority,
                             Collection<?> messages,
                             Map<String, Object> headers) {
        messages.forEach(message -> broadcast(exchangeName, queueName, routingKey, dlk, priority, message, headers));
    }

    /**
     * @param <T>        event to be published
     * @param event      the event you want to publish
     * @param priority   priority given to the event
     * @param channel    queue and exchange configuration to publish to
     * @param purgeQueue true to purge queue if already exists. Useful in tests.
     */
    protected <T> void publish(T event, int priority, AmqpChannel channel, boolean purgeQueue) {
        LOGGER.debug("Publishing event {} (Target : {}, WorkerMode : {} )",
                     event.getClass(),
                     channel.getTarget(),
                     channel.getWorkerMode());
        String tenant = resolveTenant();
        if (tenant != null) {
            publish(resolveVirtualHost(tenant), tenant, event, priority, channel, purgeQueue);
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
     * @param virtualHost virtual host for current tenant
     * @param tenant
     * @param <T>         event to be published
     * @param priority    priority given to the event
     * @param channel     Channel configuration for exchange/queue/binding
     * @param purgeQueue  true to purge queue if already exists. Useful in tests.
     */
    protected final <T> void publish(String virtualHost,
                                     String tenant,
                                     T event,
                                     int priority,
                                     AmqpChannel channel,
                                     boolean purgeQueue) {

        try {
            // Bind the connection to the right vHost (i.e. tenant to publish the message)
            rabbitVirtualHostAdmin.bind(virtualHost);

            // Declare AMQP elements for first publication
            ConcurrentMap<String, ExchangeAndRoutingKey> exchangesAndRoutingKeysByEvent = exchangesAndRoutingKeysByEventPerTenant.computeIfAbsent(
                tenant,
                key -> new ConcurrentHashMap<>());
            ExchangeAndRoutingKey er = exchangesAndRoutingKeysByEvent.computeIfAbsent(channel.getExchangeName()
                                                                                             .orElse(channel.getEventType()
                                                                                                            .getName()),
                                                                                      key -> {
                                                                                          amqpAdmin.declareDeadLetter();

                                                                                          // Declare exchange
                                                                                          Exchange exchange = amqpAdmin.declareExchange(
                                                                                              channel);

                                                                                          if (WorkerMode.UNICAST.equals(
                                                                                              channel.getWorkerMode())) {
                                                                                              // Direct exchange needs a specific queue, a binding between this queue and exchange containing a
                                                                                              // specific routing key
                                                                                              Queue queue = amqpAdmin.declareQueue(
                                                                                                  tenant,
                                                                                                  channel);
                                                                                              if (purgeQueue) {
                                                                                                  amqpAdmin.purgeQueue(
                                                                                                      queue.getName(),
                                                                                                      false);
                                                                                              }
                                                                                              amqpAdmin.declareBinding(
                                                                                                  queue,
                                                                                                  exchange,
                                                                                                  channel.getWorkerMode(),
                                                                                                  Optional.empty());
                                                                                              return ExchangeAndRoutingKey.of(
                                                                                                  exchange.getName(),
                                                                                                  amqpAdmin.getRoutingKey(
                                                                                                      Optional.of(queue),
                                                                                                      channel.getWorkerMode(),
                                                                                                      Optional.empty()));
                                                                                          } else if (WorkerMode.BROADCAST.equals(
                                                                                              channel.getWorkerMode())) {
                                                                                              // Routing key useless ... always skipped with a fanout exchange
                                                                                              return ExchangeAndRoutingKey.of(
                                                                                                  exchange.getName(),
                                                                                                  amqpAdmin.getRoutingKey(
                                                                                                      Optional.empty(),
                                                                                                      channel.getWorkerMode(),
                                                                                                      channel.getRoutingKey()));
                                                                                          } else {
                                                                                              String errorMessage = String.format(
                                                                                                  "Unexpected worker mode : %s.",
                                                                                                  channel.getWorkerMode());
                                                                                              LOGGER.error(errorMessage);
                                                                                              throw new IllegalArgumentException(
                                                                                                  errorMessage);
                                                                                          }

                                                                                      });

            // Publish
            LOGGER.debug("Publishing message on {}/{}", er.exchange, er.routingKey);
            publishMessageByTenant(tenant, er.exchange, er.routingKey, event, priority, null);
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
    }

    /**
     * Publish event in tenant virtual
     *
     * @param <T>          event type
     * @param tenant       tenant
     * @param exchangeName {@link Exchange} name
     * @param routingKey   routing key (really useful for direct exchange).
     * @param event        the event to publish
     * @param priority     the event priority
     * @param headers      additional headers
     */
    private final <T> void publishMessageByTenant(String tenant,
                                                  String exchangeName,
                                                  String routingKey,
                                                  T event,
                                                  int priority,
                                                  Map<String, Object> headers) {

        // routing key is unnecessary for fanout exchanges but is for direct exchanges
        rabbitTemplate.convertAndSend(exchangeName, routingKey, event, message -> {
            MessageProperties messageProperties = message.getMessageProperties();

            // Add default tenant if missing
            if (messageProperties.getHeader(AmqpConstants.REGARDS_TENANT_HEADER) == null) {
                messageProperties.setHeader(AmqpConstants.REGARDS_TENANT_HEADER, tenant);
            }

            // Add headers from parameter
            if (headers != null) {
                headers.forEach(messageProperties::setHeader);
            }

            // Add headers from event
            if (IMessagePropertiesAware.class.isAssignableFrom(event.getClass())) {
                MessageProperties mp = ((IMessagePropertiesAware) event).getMessageProperties();
                if (mp != null) {
                    mp.getHeaders().forEach(messageProperties::setHeader);
                }
            }

            messageProperties.setPriority(priority);
            return new Message(message.getBody(), messageProperties);
        });
    }

    private static class ExchangeAndRoutingKey {

        public final String exchange;

        public final String routingKey;

        private ExchangeAndRoutingKey(String exchange, String routingKey) {
            this.exchange = exchange;
            this.routingKey = routingKey;
        }

        public static ExchangeAndRoutingKey of(String exchange, String routingKey) {
            return new ExchangeAndRoutingKey(exchange, routingKey);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if ((o == null) || (getClass() != o.getClass())) {
                return false;
            }
            ExchangeAndRoutingKey that = (ExchangeAndRoutingKey) o;
            return Objects.equals(exchange, that.exchange) && Objects.equals(routingKey, that.routingKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(exchange, routingKey);
        }
    }

    @Override
    public void initExchange(Set<String> tenants, Class<? extends ISubscribable> event) {
        tenants.stream().map(this::resolveVirtualHost).distinct().forEach(tenant -> {
            try {
                rabbitVirtualHostAdmin.bind(resolveVirtualHost(tenant));
                amqpAdmin.declareExchange(AmqpChannel.build(event));
            } finally {
                rabbitVirtualHostAdmin.unbind();
            }
        });
    }
}
