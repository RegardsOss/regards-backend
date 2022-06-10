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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.amqp.batch.RabbitBatchMessageListener;
import fr.cnes.regards.framework.amqp.configuration.AmqpChannel;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsErrorHandler;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.RabbitMessageListenerAdapter;
import fr.cnes.regards.framework.amqp.event.*;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.MessageConverter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Common subscriber methods
 *
 * @author Marc Sordi
 */
public abstract class AbstractSubscriber implements ISubscriberContract {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSubscriber.class);

    /**
     * method from {@link fr.cnes.regards.framework.amqp.domain.IHandler}
     */
    private static final String DEFAULT_HANDLING_METHOD = "handleAndLog";

    /**
     * configuration allowing us to declare virtual host using http api and get a unique name for the instance
     */
    protected final IAmqpAdmin amqpAdmin;

    /**
     * Allows to retrieve {@link ConnectionFactory} per tenant
     */
    protected final IRabbitVirtualHostAdmin virtualHostAdmin;

    /**
     * Reference to running listeners per handlers and virtual hosts
     */
    protected final Map<String, Map<String, SimpleMessageListenerContainer>> listeners;

    /**
     * Reference to events managed by handlers
     */
    protected final Map<String, Class<?>> handledEvents;

    /**
     * Reference to custom exchange/queue configuration as Pair<QueueName,ExchangeName> by handler
     */
    protected final Map<String, Pair<Optional<String>, Optional<String>>> handledQueueExchangeNames;

    /**
     * Reference to instances of handlers
     */
    protected final Map<String, IHandler<? extends ISubscribable>> handlerInstances;

    /**
     * bean handling the conversion using either Jackson or Gson
     */
    private final MessageConverter jsonMessageConverters;

    private final RegardsErrorHandler errorHandler;

    private final String microserviceName;

    private final IInstancePublisher instancePublisher;

    private final IPublisher publisher;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ITenantResolver tenantResolver;

    protected AbstractSubscriber(IRabbitVirtualHostAdmin virtualHostAdmin,
                                 IAmqpAdmin amqpAdmin,
                                 MessageConverter jsonMessageConverters,
                                 RegardsErrorHandler errorHandler,
                                 String microserviceName,
                                 IInstancePublisher instancePublisher,
                                 IPublisher publisher,
                                 IRuntimeTenantResolver runtimeTenantResolver,
                                 ITenantResolver tenantResolver) {
        this.virtualHostAdmin = virtualHostAdmin;
        this.amqpAdmin = amqpAdmin;
        this.jsonMessageConverters = jsonMessageConverters;
        this.listeners = new HashMap<>();
        this.handledEvents = new HashMap<>();
        this.handlerInstances = new HashMap<>();
        this.handledQueueExchangeNames = new HashMap<>();
        this.errorHandler = errorHandler;
        this.microserviceName = microserviceName;
        this.instancePublisher = instancePublisher;
        this.publisher = publisher;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.tenantResolver = tenantResolver;
    }

    @Override
    public void unsubscribeFromAll(boolean fast) {
        for (Map.Entry<String, Class<?>> handleEvent : handledEvents.entrySet()) {
            // Retrieve handler managing event to unsubscribe
            unsubscribe(handleEvent.getKey(), fast);
        }
        handledEvents.clear();
    }

    @Override
    public <T extends ISubscribable> void unsubscribeFrom(Class<T> eventType, boolean fast) {
        for (Map.Entry<String, Class<?>> handleEvent : handledEvents.entrySet()) {
            if (handleEvent.getValue().equals(eventType)) {
                unsubscribe(handleEvent.getKey(), fast);
            }
        }
    }

    private void unsubscribe(String handlerClassName, boolean fast) {
        LOGGER.info("AMQP Subscriber : Unsubscribe from {}", handlerClassName);
        // Retrieve listeners for current handler
        Map<String, SimpleMessageListenerContainer> tenantContainers = listeners.remove(handlerClassName);
        // In case unsubscribeFrom has been called too late
        if (tenantContainers != null) {
            // Stop listeners
            for (SimpleMessageListenerContainer container : tenantContainers.values()) {
                if (fast) {
                    // Force fast dying consumer, as they do not matter anymore
                    container.setShutdownTimeout(5 / 1000);
                }
                container.stop();
            }
        }
        LOGGER.info("END AMQP Subscriber : Unsubscribe from {}", handlerClassName);
    }

    @Override
    public <T extends ISubscribable> void subscribeTo(Class<T> eventType, IHandler<T> receiver) {
        AmqpChannel channel = AmqpChannel.build(eventType)
                                         .autoDelete(EventUtils.isAutoDeleteQueue(eventType))
                                         .forHandler(receiver);
        subscribeTo(receiver, channel, false);
    }

    @Override
    public <T extends ISubscribable> void subscribeTo(Class<T> eventType,
                                                      IHandler<T> receiver,
                                                      String queueName,
                                                      String exchangeName) {
        AmqpChannel channel = AmqpChannel.build(eventType)
                                         .forHandler(receiver)
                                         .autoDelete(EventUtils.isAutoDeleteQueue(eventType))
                                         .exchange(exchangeName)
                                         .queue(queueName);
        subscribeTo(receiver, channel, false);
    }

    @Override
    public <T extends ISubscribable> void subscribeTo(Class<T> eventType,
                                                      IHandler<T> receiver,
                                                      String queueName,
                                                      String exchangeName,
                                                      boolean purgeQueue) {
        AmqpChannel channel = AmqpChannel.build(eventType)
                                         .forHandler(receiver)
                                         .autoDelete(EventUtils.isAutoDeleteQueue(eventType))
                                         .exchange(exchangeName)
                                         .queue(queueName);
        subscribeTo(receiver, channel, true);
    }

    @Override
    public <E extends ISubscribable> void subscribeTo(Class<E> eventType, IHandler<E> receiver, boolean purgeQueue) {
        AmqpChannel channel = AmqpChannel.build(eventType)
                                         .forHandler(receiver)
                                         .autoDelete(EventUtils.isAutoDeleteQueue(eventType));
        subscribeTo(receiver, channel, purgeQueue);
    }

    @Override
    public void purgeAllQueues(String tenant) {
        if (virtualHostAdmin != null) {
            LOGGER.info("Purging queues for {} handlers", handlerInstances.size());
            handlerInstances.values().parallelStream().forEach(handler -> cleanAMQPQueue(handler, tenant));
            LOGGER.info("End purging queues for {} handlers", handlerInstances.size());
        }
    }

    private void cleanAMQPQueue(IHandler<?> handler, String tenant) {
        Class<?> event = null;
        Target target = null;
        WorkerMode mode = null;
        Type[] genericInterfaces = handler.getClass().getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                String interf = ((ParameterizedType) genericInterface).getRawType().getTypeName();
                if (interf.equals(IBatchHandler.class.getName()) || interf.equals(IHandler.class.getName())) {
                    event = (Class<?>) ((ParameterizedType) genericInterface).getActualTypeArguments()[0];
                    Event annotation = event.getAnnotation(Event.class);
                    target = annotation.target();
                    mode = annotation.mode();
                    break;
                }
            }
        }
        if (event == null) {
            LOGGER.error("Unable to clean queue for heandler {}", handler.getClass().getName());
        } else {
            purgeQueueByName(tenant, handler, event, mode, target);
        }
    }

    /**
     * Allows to purge a queue content by generating queue name from given parameters
     *
     * @param tenant  String
     * @param handler {@link Class} of {@link IHandler}
     * @param event   {@link Class} of {@link Event}
     * @param mode    {@link WorkerMode}
     * @param target  {@link Target}
     */
    private void purgeQueueByName(String tenant, IHandler<?> handler, Class<?> event, WorkerMode mode, Target target) {
        try {
            virtualHostAdmin.bind(AmqpChannel.AMQP_MULTITENANT_MANAGER);
            String queueName;
            if (mode == WorkerMode.BROADCAST) {
                queueName = amqpAdmin.getSubscriptionQueueName((Class<? extends IHandler<?>>) handler.getClass(),
                                                               target);
            } else {
                queueName = amqpAdmin.getUnicastQueueName(tenant, event, target);
            }
            LOGGER.info("Purging queue {} --> for {},{},{}",
                        queueName,
                        event.getName(),
                        target.toString(),
                        mode.toString());
            amqpAdmin.purgeQueue(queueName, false);
        } catch (AmqpIOException e) {
            //todo
        } finally {
            virtualHostAdmin.unbind();
        }
    }

    @Override
    public <E extends ISubscribable> void purgeQueue(Class<E> eventType,
                                                     Class<? extends IHandler<E>> handlerType,
                                                     Optional<String> queueName) {

        Set<String> tenants = resolveTenants();
        for (final String tenant : tenants) {
            String virtualHost = resolveVirtualHost(tenant);
            try {
                virtualHostAdmin.bind(virtualHost);
                AmqpChannel channel = AmqpChannel.build(eventType,
                                                        EventUtils.getWorkerMode(eventType),
                                                        EventUtils.getTargetRestriction(eventType));
                Queue queue = amqpAdmin.declareQueue(tenant, channel.forHandlerType(handlerType));
                amqpAdmin.purgeQueue(queue.getName(), false);
            } finally {
                virtualHostAdmin.unbind();
            }
        }
    }

    /**
     * Initialize any necessary container to listen to specified event using
     * AbstractSubscriber#initializeSimpleMessageListenerContainer(Class, String, IHandler, WorkerMode, Target)
     *
     * @param <E>        event type to which we subscribe
     * @param <H>        handler type
     * @param handler    the POJO defining the method handling the corresponding event connection factory from context
     * @param channel    Channel configuration for exchange/queue/binding
     * @param purgeQueue true to purge queue if already exists
     */
    protected <E extends ISubscribable, H extends IHandler<E>> void subscribeTo(H handler,
                                                                                AmqpChannel channel,
                                                                                boolean purgeQueue) {

        LOGGER.info("Subscribing to event {} with target {} and mode {} - {}",
                    channel.getEventType().getName(),
                    channel.getTarget(),
                    channel.getWorkerMode(),
                    channel.getQueueName().orElse(""));

        Set<String> tenants = resolveTenants();

        Map<String, SimpleMessageListenerContainer> vhostsContainers = new ConcurrentHashMap<>();

        String handlerClassName = handler.getClass().getName();
        if (!listeners.containsKey(handlerClassName)) {
            listeners.put(handlerClassName, vhostsContainers);
            handledEvents.put(handlerClassName, channel.getEventType());
            handlerInstances.put(handlerClassName, handler);
            handledQueueExchangeNames.put(handlerClassName, Pair.of(channel.getQueueName(), channel.getExchangeName()));
        }

        Multimap<String, Queue> vhostQueues = ArrayListMultimap.create();

        for (final String tenant : tenants) {
            String virtualHost = resolveVirtualHost(tenant);
            // Declare AMQP elements
            Queue queue = declareElements(virtualHost, tenant, channel, purgeQueue);
            vhostQueues.put(virtualHost, queue);
        }

        // Init listeners
        for (Map.Entry<String, Collection<Queue>> entry : vhostQueues.asMap().entrySet()) {
            declareListener(entry.getKey(), jsonMessageConverters, handler, entry.getValue(), channel.getEventType());
        }
    }

    /**
     * @return the tenant on which we have to subscribe to the event
     */
    protected abstract Set<String> resolveTenants();

    /**
     * @param tenant current tenant
     * @return the virtual host on which we have to publish the event according to the tenant
     */
    protected abstract String resolveVirtualHost(String tenant);

    /**
     * Declare exchange, queue and binding on a virtual host<br/>
     * Queue name is queueName if provided or is computed with {@link WorkerMode},  {@link Target}
     *
     * @param virtualHost virtual host
     * @param tenant
     * @param channel     Channel configuration for exchange/queue/binding
     * @param purgeQueue  true to purge queue if already exists
     */
    protected Queue declareElements(String virtualHost, String tenant, AmqpChannel channel, boolean purgeQueue) {
        Queue queue;
        try {
            virtualHostAdmin.bind(virtualHost);
            amqpAdmin.declareDeadLetter();
            queue = amqpAdmin.declareQueue(tenant, channel);
            if (purgeQueue) {
                amqpAdmin.purgeQueue(queue.getName(), false);
            }
            // If queue name is not provided, the queueName and the exchange are created with generated names
            // If queueName is provided and exchange name is not, so the queue is created without binding with any exchange.
            // If queueName and exchange name are provided, queueName is bind to the given exchange
            if (!channel.getQueueName().isPresent() || channel.getExchangeName().isPresent()) {
                Exchange exchange = amqpAdmin.declareExchange(channel);
                amqpAdmin.declareBinding(queue, exchange, channel.getWorkerMode(), channel.getRoutingKey());
            }
        } finally {
            virtualHostAdmin.unbind();
        }
        return queue;
    }

    /**
     * Declare listener according to virtual host, handler and queue(s).
     *
     * @param virtualHost virtual host
     * @param handler     event handler
     * @param queues      queues to listen to
     */
    protected void declareListener(String virtualHost,
                                   MessageConverter messageConverter,
                                   IHandler<? extends ISubscribable> handler,
                                   Collection<Queue> queues,
                                   final Class<?> eventType) {

        // Prevent redundant listener
        String handlerClassName = handler.getClass().getName();
        Map<String, SimpleMessageListenerContainer> vhostsContainers = listeners.get(handlerClassName);
        // Virtual host already registered, just add queues to current container
        if (vhostsContainers.containsKey(virtualHost)) {
            LOGGER.warn("Handler {} that handles {} events already defined for virtual host {}",
                        handlerClassName,
                        eventType.getName(),
                        virtualHost);
            // Add missing queues errorHandler
            SimpleMessageListenerContainer container = vhostsContainers.get(virtualHost);
            String[] existingQueues = container.getQueueNames();
            Set<String> newQueueNames = new HashSet<>();
            boolean exists;
            for (Queue queue : queues) {
                if (Arrays.stream(existingQueues).noneMatch(q -> q.equals(queue.getName()))) {
                    newQueueNames.add(queue.getName());
                }
            }
            // Add new queues to the existing container
            if (!newQueueNames.isEmpty()) {
                container.addQueueNames(newQueueNames.toArray(new String[0]));
            }
            return;
        }

        // Retrieve tenant vhost connection factory
        ConnectionFactory connectionFactory = virtualHostAdmin.getVhostConnectionFactory(virtualHost);

        // Init container
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        if (!eventType.equals(NotificationEvent.class)) {
            // Do not send notification event on notification event error. (prevent infinite loop)
            container.setErrorHandler(errorHandler);
        }

        if (handler instanceof IBatchHandler) {
            container.setChannelTransacted(false);
            container.setAcknowledgeMode(AcknowledgeMode.MANUAL);

            IBatchHandler<?> batchHandler = (IBatchHandler<?>) handler;
            container.setConsumerBatchEnabled(true);
            container.setDeBatchingEnabled(true); // Required if consumer batch enabled is true
            container.setBatchSize(batchHandler.getBatchSize());
            container.setPrefetchCount(batchHandler.getBatchSize());
            container.setReceiveTimeout(batchHandler.getReceiveTimeout());
            MessageListener batchListener = new RabbitBatchMessageListener(amqpAdmin,
                                                                           microserviceName,
                                                                           instancePublisher,
                                                                           publisher,
                                                                           runtimeTenantResolver,
                                                                           tenantResolver,
                                                                           messageConverter,
                                                                           batchHandler);
            container.setMessageListener(batchListener);
        } else {
            container.setChannelTransacted(true);
            container.setDefaultRequeueRejected(false);
            MessageListenerAdapter messageListener = new RabbitMessageListenerAdapter(handler, DEFAULT_HANDLING_METHOD);
            messageListener.setMessageConverter(messageConverter);
            container.setMessageListener(messageListener);
        }

        // Prevent duplicate queue
        Set<String> queueNames = new HashSet<>();
        queues.forEach(q -> queueNames.add(q.getName()));
        container.addQueueNames(queueNames.toArray(new String[0]));
        vhostsContainers.put(virtualHost, container);

        container.start();
    }

    /**
     * Add tenant listener to existing subscribers
     *
     * @param tenant new tenant to manage
     */
    protected void addTenantListeners(String tenant) {
        for (Map.Entry<String, Map<String, SimpleMessageListenerContainer>> entry : listeners.entrySet()) {
            String handlerClass = entry.getKey();
            Class<?> eventType = handledEvents.get(handlerClass);
            IHandler<? extends ISubscribable> handler = handlerInstances.get(handlerClass);
            Pair<Optional<String>, Optional<String>> queueExchangeName = handledQueueExchangeNames.get(handlerClass);
            String virtualHost = resolveVirtualHost(tenant);

            // Declare AMQP elements
            AmqpChannel channel = AmqpChannel.build(eventType)
                                             .forHandler(handler)
                                             .autoDelete(EventUtils.isAutoDeleteQueue(eventType));
            queueExchangeName.getLeft().ifPresent(channel::queue);
            queueExchangeName.getRight().ifPresent(channel::exchange);

            // Declare AMQP elements
            Queue queue = declareElements(virtualHost, tenant, channel, false);
            // Manage listeners
            List<Queue> queues = new ArrayList<>();
            queues.add(queue);
            declareListener(virtualHost, jsonMessageConverters, handler, queues, eventType);
        }
    }

    /**
     * Retrieve listeners for specified handler. For test purpose!
     *
     * @param handler event handler
     * @return all listeners by virtual host or <code>null</code>
     */
    public Map<String, SimpleMessageListenerContainer> getListeners(IHandler<? extends ISubscribable> handler) {
        String handlerClassName = handler.getClass().getName();
        if (listeners.containsKey(handlerClassName)) {
            return listeners.get(handlerClassName);
        }
        return null;
    }

}
