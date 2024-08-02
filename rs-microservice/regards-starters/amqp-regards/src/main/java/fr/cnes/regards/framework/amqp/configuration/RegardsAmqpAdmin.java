/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * This class manage tenant AMQP administration. Each tenant is hosted in an AMQP virtual host.<br/>
 *
 * @author svissier
 * @author Marc Sordi
 */
public class RegardsAmqpAdmin implements IAmqpAdmin, InitializingBean {

    public static final String UNICAST_BASE_EXCHANGE_NAME = "unicast";

    public static final String BROADCAST_BASE_EXCHANGE_NAME = "broadcast";

    /**
     * Default routing key
     */
    public static final String DEFAULT_ROUTING_KEY = "";

    /**
     * This constant allows to defined a message to send with a high priority
     */
    public static final Integer MAX_PRIORITY = 255;

    public static final String REGARDS_PREFIX = "regards";

    public static final String DLX_SUFFIX = ".DLX";

    public static final String DLQ_SUFFIX = ".DLQ";

    public static final String RETRY_SUFFIX = ".retry";

    public static final String REGARDS_DLX = REGARDS_PREFIX + DLX_SUFFIX;

    public static final String REGARDS_DLQ = REGARDS_PREFIX + DLQ_SUFFIX;

    public static final String REGARDS_RETRY_EXCHANGE = REGARDS_PREFIX + RETRY_SUFFIX;

    private static final String DOT = ".";

    /*
     * Namespace used in queue and exchange naming
     */
    private final String namespace;

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

    public RegardsAmqpAdmin(String namespace, String microserviceTypeId, String microserviceInstanceId) {
        this.namespace = namespace;
        this.microserviceTypeId = microserviceTypeId;
        this.microserviceInstanceId = microserviceInstanceId;
    }

    @Override
    public void afterPropertiesSet() {
        // Fallback if no microservice instance properties was given
        if (microserviceTypeId == null) {
            microserviceTypeId = microserviceName;
        }
        if (microserviceInstanceId == null) {
            this.instanceIdGenerated = true;
            this.microserviceInstanceId = UUID.randomUUID().toString();
        }
    }

    private String getUnicastNamespace() {
        return namespace + DOT + UNICAST_BASE_EXCHANGE_NAME;
    }

    private String getBroadcastNamespace() {
        return namespace + DOT + BROADCAST_BASE_EXCHANGE_NAME;
    }

    @Override
    public Exchange declareExchange(AmqpChannel channel) {

        Exchange exchange;
        switch (channel.getWorkerMode()) {
            case UNICAST:
                exchange = ExchangeBuilder.directExchange(channel.getExchangeName().orElse(getUnicastExchangeName()))
                                          .durable(true)
                                          .build();
                break;
            case BROADCAST:
                if (channel.getRoutingKey().isPresent()) {
                    exchange = ExchangeBuilder.topicExchange(channel.getExchangeName()
                                                                    .orElse(getBroadcastExchangeName(channel.getEventType()
                                                                                                            .getName(),
                                                                                                     channel.getTarget())))
                                              .durable(true)
                                              .build();
                } else {
                    exchange = ExchangeBuilder.fanoutExchange(channel.getExchangeName()
                                                                     .orElse(getBroadcastExchangeName(channel.getEventType()
                                                                                                             .getName(),
                                                                                                      channel.getTarget())))
                                              .durable(true)
                                              .build();
                }
                break;
            default:
                throw new EnumConstantNotPresentException(WorkerMode.class, channel.getWorkerMode().name());
        }

        rabbitAdmin.declareExchange(exchange);
        return exchange;
    }

    @Override
    public String getUnicastExchangeName() {
        return getUnicastNamespace();
    }

    @Override
    public String getBroadcastExchangeName(String eventType, Target target) {
        StringBuilder builder = new StringBuilder();
        builder.append(getBroadcastNamespace());
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
    public void declareDeadLetter() {
        Exchange dlx = declareDeadLetterDefaultExchange();
        //create DLQ(Dead Letter Queue)
        Queue dlq = QueueBuilder.durable(REGARDS_DLQ).maxPriority(MAX_PRIORITY).build();
        rabbitAdmin.declareQueue(dlq);
        Binding binding = BindingBuilder.bind(dlq).to(dlx).with(REGARDS_DLQ).noargs();
        rabbitAdmin.declareBinding(binding);
    }

    private Exchange declareDeadLetterDefaultExchange() {
        // Create DLX (Dead Letter eXchange)
        Exchange dlx = ExchangeBuilder.topicExchange(REGARDS_DLX).durable(true).build();
        rabbitAdmin.declareExchange(dlx);
        return dlx;
    }

    private void declareDedicatedDeadLetter(String dlq, String dlrk) {
        Exchange dlx = declareDeadLetterDefaultExchange();
        // Create dedicated DLQ (Dead Letter Queue)
        Queue queue = QueueBuilder.durable(dlq).maxPriority(MAX_PRIORITY).build();
        rabbitAdmin.declareQueue(queue);
        // Bind
        Binding binding = BindingBuilder.bind(queue).to(dlx).with(dlrk).noargs();
        rabbitAdmin.declareBinding(binding);
    }

    @Override
    public void declareRetryExchange() {
        rabbitAdmin.declareExchange(getRetryExchange());
    }

    private DirectExchange getRetryExchange() {
        return ExchangeBuilder.directExchange(REGARDS_RETRY_EXCHANGE).delayed().build();

    }

    @Override
    public Queue declareQueue(String tenant, AmqpChannel channel) {

        // Default DLQ values
        String dlx = REGARDS_DLX;
        String dlrk = channel.getDeadLetterQueueRoutingKey().orElse(REGARDS_DLQ);

        QueueBuilder builder;
        switch (channel.getWorkerMode()) {
            case UNICAST -> {
                // Useful for publishing unicast event and subscribe to a unicast exchange
                builder = QueueBuilder.durable(channel.getQueueName()
                                                      .orElse(getUnicastQueueName(tenant,
                                                                                  channel.getEventType(),
                                                                                  channel.getTarget())));
                if (channel.isDeclareDlq()) {
                    builder = builder.deadLetterExchange(dlx).deadLetterRoutingKey(dlrk).maxPriority(MAX_PRIORITY);
                }
            }
            case BROADCAST -> {
                // Allows to subscribe to a broadcast exchange
                if (channel.getHandlerType().isPresent()) {

                    String qn = channel.getQueueName()
                                       .orElse(getSubscriptionQueueName(channel.getHandlerType().get(),
                                                                        channel.getTarget()));

                    builder = QueueBuilder.durable(qn).maxPriority(MAX_PRIORITY);

                    // If  target is ALL, queue name is specific for the current instance of microservice.
                    // The instance id is a random uuid so the queue must be autoDelete to be deleted after instance stop.
                    // Auto delete option force rabbitmq server to delete queues with no consumer associated.
                    if (Target.ALL.equals(channel.getTarget()) || Target.MICROSERVICE.equals(channel.getTarget())) {
                        builder.autoDelete();
                    }

                    // Needs a DLQ
                    if (channel.isDeclareDlq()) {
                        // Dedicated DLQ creation
                        if (channel.isDedicatedDLQEnabled()) {
                            dlrk = channel.getDeadLetterQueueRoutingKey().orElse(getDedicatedDLRKFromQueueName(qn));
                            declareDedicatedDeadLetter(getDedicatedDLQFromQueueName(qn), dlrk);
                        }
                        builder = builder.deadLetterExchange(dlx).deadLetterRoutingKey(dlrk);
                    }
                } else {
                    throw new IllegalArgumentException("Missing event handler for broadcasted event");
                }
            }
            default -> throw new EnumConstantNotPresentException(WorkerMode.class, channel.getWorkerMode().name());
        }
        if (channel.isAutoDeleteQueue()) {
            builder = builder.autoDelete();
        }

        Queue queue = builder.build();
        rabbitAdmin.declareQueue(queue);

        // bind the retry exchange to the queue if the option is activated.
        if (channel.isRetryEnabled()) {
            rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(getRetryExchange()).withQueueName());
        }

        return queue;
    }

    /**
     * Unicast publish queue name build for {@link IPollable} or {@link ISubscribable} event according to its
     * {@link Target} restriction.<br/>
     * Tenant is used for working queues naming to prevent starvation of a project. Otherwise, some projects may
     * monopolize a single multitenant queue!
     *
     * @param tenant    tenant (useful for queue naming in single virtual host and compatible with multiple virtual hosts)
     * @param eventType event type
     * @param target    {@link Target}
     * @return queue name
     */
    @Override
    public String getUnicastQueueName(String tenant, Class<?> eventType, Target target) {
        if (Target.ONE_PER_MICROSERVICE_TYPE.equals(target)) {
            throw new IllegalArgumentException(String.format("Target %s not supported", target));
        }

        StringBuilder builder = new StringBuilder();
        builder.append(getUnicastNamespace());
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
     *
     * @param handlerType event handler
     * @return queue name
     */
    @Override
    public String getSubscriptionQueueName(Class<? extends IHandler<?>> handlerType, Target target) {
        StringBuilder builder = new StringBuilder();
        builder.append(getBroadcastNamespace());
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

    @Override
    public String getDedicatedDLQFromQueueName(String queueName) {
        return queueName + DLQ_SUFFIX;
    }

    @Override
    public String getDedicatedDLRKFromQueueName(String queueName) {
        return getDedicatedDLQFromQueueName(queueName);
    }

    @Override
    public Binding declareBinding(Queue queue,
                                  Exchange exchange,
                                  WorkerMode workerMode,
                                  Optional<String> broadcastRoutingKey) {

        Binding binding;
        switch (workerMode) {
            case UNICAST:
                binding = BindingBuilder.bind(queue)
                                        .to((DirectExchange) exchange)
                                        .with(getRoutingKey(Optional.of(queue), WorkerMode.UNICAST, Optional.empty()));
                break;
            case BROADCAST:
                if (broadcastRoutingKey.isPresent()) {
                    binding = BindingBuilder.bind(queue)
                                            .to((TopicExchange) exchange)
                                            .with(getRoutingKey(Optional.of(queue),
                                                                WorkerMode.BROADCAST,
                                                                broadcastRoutingKey));
                } else {
                    binding = BindingBuilder.bind(queue).to((FanoutExchange) exchange);
                }

                break;
            default:
                throw new EnumConstantNotPresentException(WorkerMode.class, workerMode.name());
        }

        rabbitAdmin.declareBinding(binding);
        return binding;
    }

    @Override
    public String getRoutingKey(Optional<Queue> queue, WorkerMode workerMode, Optional<String> broadcastRoutingKey) {
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
                if (broadcastRoutingKey.isPresent()) {
                    routingKey = broadcastRoutingKey.get();
                } else {
                    routingKey = DEFAULT_ROUTING_KEY;
                }
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
    public boolean isQueueEmpty(String queueName) {
        QueueInformation queueInfo = rabbitAdmin.getQueueInfo(queueName);
        return queueInfo.getMessageCount() == 0;
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

    public String getDefaultDLXName() {
        return REGARDS_DLX;
    }

    public String getDefaultDLQName() {
        return REGARDS_DLQ;
    }

    @Override
    public String getRetryExchangeName() {
        return REGARDS_RETRY_EXCHANGE;
    }
}
