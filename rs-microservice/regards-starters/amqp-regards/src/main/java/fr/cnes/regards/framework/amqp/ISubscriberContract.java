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

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.ISubscribable;

import java.util.Optional;

/**
 * {@link ISubscriberContract} allows to subscribe to {@link ISubscribable} events. This interface represents the common
 * subscriber contract whether we are in a multitenant or an instance context.
 * @author Sylvain Vissière-Guérinet
 * @author Sébastien Binda
 * @author Marc Sordi
 */
public interface ISubscriberContract {

    /**
     * Subscribe to this {@link ISubscribable} event
     * @param <E> {@link ISubscribable} event
     * @param eventType {@link ISubscribable} event
     * @param receiver event {@link IHandler}
     */
    <E extends ISubscribable> void subscribeTo(Class<E> eventType, IHandler<E> receiver);

    /**
     * Subscribe to the given queueName to listen for {@link ISubscribable} events<br/>
     * <ul>
     *     <li>The queue is created if missing</li>
     *     <li>The exchange is created if missing</li>
     *     <li>The queue is bind to the exchange with UNICAST(routingKey=queueName) or BROADCAST</li>
     * </ul>
     *
     * @param <E> {@link ISubscribable} event
     * @param eventType {@link ISubscribable} event
     * @param receiver event {@link IHandler}
     * @param queueName Name of the queue to listen for
     * @param exchangeName Name of the exchange to create and to bind to the newly subscribe queue.
     */
    <E extends ISubscribable> void subscribeTo(Class<E> eventType, IHandler<E> receiver, String queueName, String exchangeName);

    /**
     * Subscribe to the given queueName to listen for {@link ISubscribable} events<br/>
     * <ul>
     *     <li>The queue is created if missing</li>
     *     <li>The exchange is created if missing</li>
     *     <li>The queue is bind to the exchange with UNICAST(routingKey=queueName) or BROADCAST</li>
     * </ul>
     *
     * @param <E> {@link ISubscribable} event
     * @param eventType {@link ISubscribable} event
     * @param receiver event {@link IHandler}
     * @param queueName Name of the queue to listen for
     * @param exchangeName Name of the exchange to create and to bind to the newly subscribe queue.
     * @param purgeQueue
     */
    <E extends ISubscribable> void subscribeTo(Class<E> eventType, IHandler<E> receiver, String queueName, String exchangeName, boolean purgeQueue);

    /**
     * Subscribe to this {@link ISubscribable} event
     * @param <E> @link ISubscribable} event
     * @param eventType {@link ISubscribable} event
     * @param receiver event {@link IHandler}
     * @param purgeQueue true to purge queue if already exists. Useful in tests.
     */
    <E extends ISubscribable> void subscribeTo(Class<E> eventType, IHandler<E> receiver, boolean purgeQueue);

    /**
     * Unsubscribe from this {@link ISubscribable} event.
     * @param <T> {@link ISubscribable} event
     * @param eventType {@link ISubscribable} event
     * @param fast When true, do not wait for proper rabbit consumer stop. Only for test
     */
    <T extends ISubscribable> void unsubscribeFrom(Class<T> eventType, boolean fast);

    /**
     * Unsubscribe handler from all events already subscribed
     *
     * @param fast When true, do not wait for proper rabbit consumer stop. Only for test
     */
    void unsubscribeFromAll(boolean fast);

    /**
     * Purge all queues based on registered handlers
     * @param tenant
     */
    void purgeAllQueues(String tenant);

    /**
     * Purge related queues (for all tenant virtual hosts). Useful for testing purpose before publishing events. Purge
     * can be done as well using {@link #subscribeTo(Class, IHandler, boolean)}
     * @param eventType {@link ISubscribable} event type
     * @param handlerType {@link IHandler} type
     * @param queueName optional queue name to purge. If not provided queue name to purge is defined by the Event/handler provided
     */
    <E extends ISubscribable> void purgeQueue(Class<E> eventType, Class<? extends IHandler<E>> handlerType,
            Optional<String> queueName);

    /**
     * Purge related queues (for all tenant virtual hosts). Useful for testing purpose before publishing events. Purge
     * can be done as well using {@link #subscribeTo(Class, IHandler, boolean)}
     * @param eventType {@link ISubscribable} event type
     * @param handlerType {@link IHandler} type
     */
    default <E extends ISubscribable> void purgeQueue(Class<E> eventType, Class<? extends IHandler<E>> handlerType) {
        purgeQueue(eventType, handlerType, Optional.empty());
    }
}
