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
package fr.cnes.regards.framework.amqp;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.event.*;
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.amqp.batch.RabbitBatchMessageListener;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsErrorHandler;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.RabbitMessageListenerAdapter;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 * Common subscriber methods
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
    protected final Map<Class<?>, Map<String, SimpleMessageListenerContainer>> listeners;

    /**
     * Reference to events managed by handlers
     */
    protected final Map<Class<?>, Class<? extends ISubscribable>> handledEvents;

    /**
     * Reference to custom exchange/queue configuration as Pair<QueueName,ExchangeName> by handler
     */
    protected final Map<Class<?>, Pair<Optional<String>, Optional<String>>> handledQueueExchangeNames;

    /**
     * Reference to instances of handlers
     */
    protected final Map<Class<?>, IHandler<? extends ISubscribable>> handlerInstances;

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

    protected AbstractSubscriber(IRabbitVirtualHostAdmin virtualHostAdmin, IAmqpAdmin amqpAdmin,
            MessageConverter jsonMessageConverters, RegardsErrorHandler errorHandler, String microserviceName,
            IInstancePublisher instancePublisher, IPublisher publisher, IRuntimeTenantResolver runtimeTenantResolver,
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
        LOGGER.info("Stopping all {} amqp listeners ...", handlerInstances.size());
        for (Map.Entry<Class<?>, Class<? extends ISubscribable>> handleEvent : handledEvents.entrySet()) {
            // Retrieve handler managing event to unsubscribe
            Class<?> handlerClass = handleEvent.getKey();
            LOGGER.info("AMQP Subscriber : Unsubscribe from {}",handlerClass.getName());
            // Retrieve listeners for current handler
            Map<String, SimpleMessageListenerContainer> tenantContainers = listeners.remove(handlerClass);
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
            LOGGER.info("END AMQP Subscriber : Unsubscribe from {}",handlerClass.getName());
        }
        handledEvents.clear();
    }

    @Override
    public <T extends ISubscribable> void unsubscribeFrom(Class<T> eventType, boolean fast) {

        LOGGER.info("Stopping all listeners for event {}", eventType.getName());

        for (Map.Entry<Class<?>, Class<? extends ISubscribable>> handleEvent : handledEvents.entrySet()) {
            if (handleEvent.getValue().equals(eventType)) {
                // Retrieve handler managing event to unsubscribe
                Class<?> handlerClass = handleEvent.getKey();
                // Retrieve listeners for current handler
                Map<String, SimpleMessageListenerContainer> tenantContainers = listeners.remove(handlerClass);
                // In case unsubscribeFrom has been called too late
                if (tenantContainers != null) {
                    // Stop listeners
                    for (SimpleMessageListenerContainer container : tenantContainers.values()) {
                        if (fast) {
                            // Force fast dying consumer, as they do not matter anymore
                            container.setShutdownTimeout(5/1000);
                        }
                        container.stop();
                    }
                }
            }
        }
    }

    @Override
    public <T extends ISubscribable> void subscribeTo(Class<T> eventType, IHandler<T> receiver) {
        subscribeTo(eventType,
                    receiver,
                    EventUtils.getWorkerMode(eventType),
                    EventUtils.getTargetRestriction(eventType),
                    EventUtils.getRoutingKey(eventType),
                    Optional.empty(),
                    Optional.empty(),
                    false);
    }

    @Override
    public <T extends ISubscribable> void subscribeTo(Class<T> eventType, IHandler<T> receiver, String queueName, String exchangeName) {
        subscribeTo(eventType,
                    receiver,
                    EventUtils.getWorkerMode(eventType),
                    EventUtils.getTargetRestriction(eventType),
                    EventUtils.getRoutingKey(eventType),
                    Optional.of(queueName),
                    Optional.of(exchangeName),
                    false);
    }

    @Override
    public <T extends ISubscribable> void subscribeTo(Class<T> eventType, IHandler<T> receiver, String queueName, String exchangeName, boolean purgeQueue) {
        subscribeTo(eventType,
                    receiver,
                    EventUtils.getWorkerMode(eventType),
                    EventUtils.getTargetRestriction(eventType),
                    EventUtils.getRoutingKey(eventType),
                    Optional.of(queueName),
                    Optional.of(exchangeName),
                    purgeQueue);
        }

    @Override
    public <E extends ISubscribable> void subscribeTo(Class<E> eventType, IHandler<E> receiver, boolean purgeQueue) {
        subscribeTo(eventType,
                    receiver,
                    EventUtils.getWorkerMode(eventType),
                    EventUtils.getTargetRestriction(eventType),
                    EventUtils.getRoutingKey(eventType),
                    Optional.empty(),
                    Optional.empty(),
                    purgeQueue);
    }

    @Override
    public void purgeAllQueues(String tenant) {
        if (virtualHostAdmin != null) {
            LOGGER.info("Purging queues for {} handlers", handlerInstances.size());
            handlerInstances.values().parallelStream()
                    .forEach(handler -> cleanAMQPQueue(handler, tenant));
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
     * @param tenant String
     * @param handler {@link Class} of {@link IHandler}
     * @param event {@link Class} of {@link Event}
     * @param mode {@link WorkerMode}
     * @param target {@link Target}
     */
    private void purgeQueueByName(String tenant, IHandler<?> handler, Class<?> event, WorkerMode mode, Target target) {
        try {
            virtualHostAdmin.bind(AmqpConstants.AMQP_MULTITENANT_MANAGER);
            String queueName;
            if (mode == WorkerMode.BROADCAST) {
                queueName = amqpAdmin.getSubscriptionQueueName((Class<? extends IHandler<?>>) handler.getClass(), target);
            } else {
                queueName = amqpAdmin.getUnicastQueueName(tenant, event, target);
            }
            LOGGER.info("Purging queue {} --> for {},{},{}", queueName, event.getName(),
                        target.toString(), mode.toString());
            amqpAdmin.purgeQueue(queueName, false);
        } catch (AmqpIOException e) {
            //todo
        } finally {
            virtualHostAdmin.unbind();
        }
    }

    @Override
    public <E extends ISubscribable> void purgeQueue(Class<E> eventType, Class<? extends IHandler<E>> handlerType, Optional<String> queueName) {

        Set<String> tenants = resolveTenants();
        for (final String tenant : tenants) {
            String virtualHost = resolveVirtualHost(tenant);
            try {
                virtualHostAdmin.bind(virtualHost);
                Queue queue = amqpAdmin.declareQueue(tenant,
                                                     eventType,
                                                     EventUtils.getWorkerMode(eventType),
                                                     EventUtils.getTargetRestriction(eventType),
                                                     Optional.of(handlerType),
                                                     queueName);
                amqpAdmin.purgeQueue(queue.getName(), false);
            } finally {
                virtualHostAdmin.unbind();
            }
        }
    }

    /**
     * Initialize any necessary container to listen to specified event using
     * AbstractSubscriber#initializeSimpleMessageListenerContainer(Class, String, IHandler, WorkerMode, Target)
     * @param <E> event type to which we subscribe
     * @param <H> handler type
     * @param eventType the event class token you want to subscribe to
     * @param handler the POJO defining the method handling the corresponding event connection factory from context
     * @param workerMode {@link WorkerMode}
     * @param target communication scope
     * @param routingKey optional routingKey. Only for broadcast mode
     * @param queueName
     * @param exchangeName
     * @param purgeQueue true to purge queue if already exists
     */
    protected <E extends ISubscribable, H extends IHandler<E>> void subscribeTo(final Class<E> eventType, H handler,
            final WorkerMode workerMode, final Target target, String routingKey, Optional<String> queueName,
            Optional<String> exchangeName, boolean purgeQueue) {

        LOGGER.info("Subscribing to event {} with target {} and mode {}", eventType.getName(), target, workerMode);

        Set<String> tenants = resolveTenants();

        Map<String, SimpleMessageListenerContainer> vhostsContainers = new ConcurrentHashMap<>();

        if (!listeners.containsKey(handler.getClass())) {
            listeners.put(handler.getClass(), vhostsContainers);
            handledEvents.put(handler.getClass(), eventType);
            handlerInstances.put(handler.getClass(), handler);
            handledQueueExchangeNames.put(handler.getClass(), Pair.of(queueName,exchangeName));
        }

        Multimap<String, Queue> vhostQueues = ArrayListMultimap.create();

        for (final String tenant : tenants) {
            String virtualHost = resolveVirtualHost(tenant);
            // Declare AMQP elements
            Queue queue = declareElements(eventType, tenant, virtualHost, handler, workerMode, target,
                                          Optional.ofNullable(routingKey), queueName, exchangeName,purgeQueue);
            vhostQueues.put(virtualHost, queue);
        }

        // Init listeners
        for (Map.Entry<String, Collection<Queue>> entry : vhostQueues.asMap().entrySet()) {
            declareListener(entry.getKey(), jsonMessageConverters, handler, entry.getValue(), eventType);
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
     * @param eventType event to listen to
     * @param tenant tenant
     * @param virtualHost virtual host
     * @param handler event handler
     * @param workerMode worker mode
     * @param target target restriction
     * @param routingKey optional routing key for broadcast mode only
     * @param queueName optional queue name to declare.
     * @param exchangeName optional exchange name to declare.
     * @param purgeQueue true to purge queue if already exists
     */
    protected Queue declareElements(Class<? extends ISubscribable> eventType, final String tenant, String virtualHost,
            IHandler<? extends ISubscribable> handler, final WorkerMode workerMode, final Target target, Optional<String> routingKey,
            Optional<String> queueName, Optional<String> exchangeName, boolean purgeQueue) {
        Queue queue;
        try {
            virtualHostAdmin.bind(virtualHost);
            amqpAdmin.declareDeadLetter();
            Optional<Class<? extends IHandler<?>>> handlerType = Optional.empty();
            if (handler != null) {
                handlerType = Optional.of(handler.getType());
            }

            if (handler instanceof IBatchHandler) {
                IBatchHandler<?> batchHandler = (IBatchHandler<?>) handler;
                queue = amqpAdmin.declareQueue(tenant,
                                               eventType,
                                               workerMode,
                                               target,
                                               handlerType,
                                               queueName,
                                               batchHandler.isDedicatedDLQEnabled(),
                                               batchHandler.getDLQRoutingKey());
            } else {
                queue = amqpAdmin.declareQueue(tenant, eventType, workerMode, target, handlerType, queueName);
            }
            if (purgeQueue) {
                amqpAdmin.purgeQueue(queue.getName(), false);
            }
            // If queue name is not provided, the queueName and the exchange are created with generated names
            // If queueName is provided and exchange name is not, so the queue is created without binding with any exchange.
            // If queueName and exchange name are provided, queueName is bind to the given exchange
            if (!queueName.isPresent() || exchangeName.isPresent()) {
                Exchange exchange = amqpAdmin.declareExchange(eventType, workerMode, target, exchangeName, routingKey);
                amqpAdmin.declareBinding(queue, exchange, workerMode, routingKey);
            }
        } finally {
            virtualHostAdmin.unbind();
        }
        return queue;
    }

    /**
     * Declare listener according to virtual host, handler and queue(s).
     * @param virtualHost virtual host
     * @param handler event handler
     * @param queues queues to listen to
     */
    protected void declareListener(String virtualHost, MessageConverter messageConverter,
            IHandler<? extends ISubscribable> handler, Collection<Queue> queues, final Class<?> eventType) {

        // Prevent redundant listener
        Map<String, SimpleMessageListenerContainer> vhostsContainers = listeners.get(handler.getClass());
        // Virtual host already registered, just add queues to current container
        if (vhostsContainers.containsKey(virtualHost)) {
            // Add missing queues errorHandler
            SimpleMessageListenerContainer container = vhostsContainers.get(virtualHost);
            String[] existingQueues = container.getQueueNames();
            Set<String> newQueueNames = new HashSet<>();
            boolean exists;
            for (Queue queue : queues) {
                exists = false;
                for (String existingQueue : existingQueues) {
                    if (queue.getName().equals(existingQueue)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
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
            MessageListener batchListener = new RabbitBatchMessageListener(amqpAdmin, microserviceName,
                    instancePublisher, publisher, runtimeTenantResolver, tenantResolver, messageConverter,
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
     * @param tenant new tenant to manage
     */
    protected void addTenantListeners(String tenant) {
        for (Map.Entry<Class<?>, Map<String, SimpleMessageListenerContainer>> entry : listeners.entrySet()) {
            Class<?> handlerClass = entry.getKey();
            Class<? extends ISubscribable> eventType = handledEvents.get(handlerClass);
            IHandler<? extends ISubscribable> handler = handlerInstances.get(handlerClass);
            Pair<Optional<String>, Optional<String>> queueExchangeName = handledQueueExchangeNames.get(handlerClass);
            String virtualHost = resolveVirtualHost(tenant);

            // Declare AMQP elements
            WorkerMode workerMode = EventUtils.getWorkerMode(eventType);
            Target target = EventUtils.getTargetRestriction(eventType);
            String routingKey = EventUtils.getRoutingKey(eventType);

            // Declare AMQP elements
            Queue queue = declareElements(eventType, tenant, virtualHost, handler, workerMode, target, Optional.ofNullable(routingKey),
                                          queueExchangeName.getLeft(),queueExchangeName.getRight(), false);
            // Manage listeners
            List<Queue> queues = new ArrayList<>();
            queues.add(queue);
            declareListener(virtualHost, jsonMessageConverters, handler, queues, eventType);
        }
    }

    /**
     * Retrieve listeners for specified handler. For test purpose!
     * @param handler event handler
     * @return all listeners by virtual host or <code>null</code>
     */
    public Map<String, SimpleMessageListenerContainer> getListeners(IHandler<? extends ISubscribable> handler) {
        if (listeners.containsKey(handler.getClass())) {
            return listeners.get(handler.getClass());
        }
        return null;
    }

}
