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
package fr.cnes.regards.framework.amqp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.EventUtils;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;

/**
 *
 * Common subscriber methods
 *
 * @author Marc Sordi
 *
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
     * bean handling the conversion using {@link com.fasterxml.jackson} 2
     */
    private final Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    /**
     * Reference to running listeners per handlers and virtual hosts
     */
    protected final Map<Class<?>, Map<String, SimpleMessageListenerContainer>> listeners;

    /**
     * Reference to events managed by handlers
     */
    protected final Map<Class<?>, Class<? extends ISubscribable>> handledEvents;

    /**
     * Reference to instances of handlers
     */
    protected final Map<Class<?>, IHandler<? extends ISubscribable>> handlerInstances;

    public AbstractSubscriber(IRabbitVirtualHostAdmin virtualHostAdmin, IAmqpAdmin amqpAdmin,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        this.virtualHostAdmin = virtualHostAdmin;
        this.amqpAdmin = amqpAdmin;
        this.jackson2JsonMessageConverter = jackson2JsonMessageConverter;
        jackson2JsonMessageConverter.setTypePrecedence(TypePrecedence.INFERRED);
        listeners = new HashMap<>();
        handledEvents = new HashMap<>();
        handlerInstances = new HashMap<>();
    }

    @Override
    public <T extends ISubscribable> void unsubscribeFrom(Class<T> eventType) {

        LOGGER.debug("Stopping all listeners for event {}", eventType.getName());

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
                        container.stop();
                    }
                }
            }
        }
    }

    @Override
    public <T extends ISubscribable> void subscribeTo(Class<T> eventType, IHandler<T> receiver) {
        subscribeTo(eventType, receiver, EventUtils.getWorkerMode(eventType),
                    EventUtils.getTargetRestriction(eventType), false);
    }

    @Override
    public <E extends ISubscribable> void subscribeTo(Class<E> eventType, IHandler<E> receiver, boolean purgeQueue) {
        subscribeTo(eventType, receiver, EventUtils.getWorkerMode(eventType),
                    EventUtils.getTargetRestriction(eventType), purgeQueue);
    }

    /**
     *
     * Initialize any necessary container to listen to specified event using
     * {@link AbstractSubscriber#initializeSimpleMessageListenerContainer(Class, String, IHandler, WorkerMode, Target)}
     *
     * @param <T>
     *            event type to which we subscribe
     * @param eventType
     *            the event class token you want to subscribe to
     * @param handler
     *            the POJO defining the method handling the corresponding event connection factory from context
     * @param workerMode
     *            {@link WorkerMode}
     * @param target
     *            communication scope
     * @param purgeQueue true to purge queue if already exists
     */
    protected <E extends ISubscribable, H extends IHandler<E>> void subscribeTo(final Class<E> eventType, H handler,
            final WorkerMode workerMode, final Target target, boolean purgeQueue) {

        LOGGER.debug("Subscribing to event {} with target {} and mode {}", eventType.getName(), target, workerMode);

        Set<String> tenants = resolveTenants();

        Map<String, SimpleMessageListenerContainer> vhostsContainers = new HashMap<>();

        if (!listeners.containsKey(handler.getClass())) {
            listeners.put(handler.getClass(), vhostsContainers);
            handledEvents.put(handler.getClass(), eventType);
            handlerInstances.put(handler.getClass(), handler);
        }

        Multimap<String, Queue> vhostQueues = ArrayListMultimap.create();

        for (final String tenant : tenants) {
            String virtualHost = resolveVirtualHost(tenant);
            // Declare AMQP elements
            Queue queue = declareElements(eventType, tenant, virtualHost, handler, workerMode, target, purgeQueue);
            vhostQueues.put(virtualHost, queue);
        }

        // Init listeners
        for (Map.Entry<String, Collection<Queue>> entry : vhostQueues.asMap().entrySet()) {
            declareListener(entry.getKey(), handler, entry.getValue());
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
     * Declare exchange, queue and binding on a virtual host
     * @param eventType event to listen to
     * @param tenant tenant
     * @param virtualHost virtual host
     * @param handler event handler
     * @param workerMode worker mode
     * @param target target restriction
     * @param purgeQueue true to purge queue if already exists
     */
    protected Queue declareElements(Class<? extends ISubscribable> eventType, final String tenant, String virtualHost,
            IHandler<? extends ISubscribable> handler, final WorkerMode workerMode, final Target target,
            boolean purgeQueue) {
        Queue queue;
        try {
            virtualHostAdmin.bind(virtualHost);
            Exchange exchange = amqpAdmin.declareExchange(eventType, workerMode, target);
            queue = amqpAdmin.declareQueue(tenant, eventType, workerMode, target, Optional.ofNullable(handler));
            if (purgeQueue) {
                amqpAdmin.purgeQueue(queue.getName(), false);
            }
            amqpAdmin.declareBinding(queue, exchange, workerMode);
        } finally {
            virtualHostAdmin.unbind();
        }
        return queue;
    }

    /**
     * Declare listener according to virtual host, handler and queue(s).
     *
     * @param virtualHost virtual host
     * @param handler event handler
     * @param queues queues to listen to
     */
    protected void declareListener(String virtualHost, IHandler<? extends ISubscribable> handler,
            Collection<Queue> queues) {

        // Prevent redundant listener
        Map<String, SimpleMessageListenerContainer> vhostsContainers = listeners.get(handler.getClass());
        // Virtual host already registered, just add queues to current container
        if (vhostsContainers.containsKey(virtualHost)) {
            // Add missing queues
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
                container.addQueueNames(newQueueNames.toArray(new String[newQueueNames.size()]));
            }
            return;
        }

        // Retrieve tenant vhost connection factory
        ConnectionFactory connectionFactory = virtualHostAdmin.getVhostConnectionFactory(virtualHost);

        // Init container
        final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        final MessageListenerAdapter messageListener = new MessageListenerAdapter(handler, DEFAULT_HANDLING_METHOD);
        messageListener.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(messageListener);

        // Prevent duplicate queue
        Set<String> queueNames = new HashSet<>();
        queues.forEach(q -> queueNames.add(q.getName()));
        container.addQueueNames(queueNames.toArray(new String[queueNames.size()]));

        vhostsContainers.put(virtualHost, container);

        container.start();
    }

    /**
     * Add tenant listener to existing subscribers
     *
     * @param tenant
     *            new tenant to manage
     */
    protected void addTenantListeners(String tenant) {
        if (listeners != null) {
            for (Map.Entry<Class<?>, Map<String, SimpleMessageListenerContainer>> entry : listeners.entrySet()) {
                Class<?> handlerClass = entry.getKey();
                Class<? extends ISubscribable> eventType = handledEvents.get(handlerClass);
                IHandler<? extends ISubscribable> handler = handlerInstances.get(handlerClass);
                String virtualHost = resolveVirtualHost(tenant);

                // Declare AMQP elements
                WorkerMode workerMode = EventUtils.getWorkerMode(eventType);
                Target target = EventUtils.getTargetRestriction(eventType);

                // Declare AMQP elements
                Queue queue = declareElements(eventType, tenant, virtualHost, handler, workerMode, target, false);
                // Manage listeners
                List<Queue> queues = new ArrayList<>();
                queues.add(queue);
                declareListener(virtualHost, handler, queues);
            }
        }
    }

    /**
     * Retrieve listeners for specified handler. For test purpose!
     * @param handler event handler
     * @return all listeners by virtual host or <code>null</code>
     *
     */
    public Map<String, SimpleMessageListenerContainer> getListeners(IHandler<? extends ISubscribable> handler) {
        if (listeners.containsKey(handler.getClass())) {
            return listeners.get(handler.getClass());
        }
        return null;
    }
}
