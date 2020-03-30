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

import java.util.List;
import java.util.Optional;

import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.ISubscribable;

/**
 * {@link IPublisherContract} allows to publish {@link ISubscribable} or {@link IPollable} events. This interface
 * represents the common publisher contract whether we are in a multitenant or an instance context.
 * @author Sylvain Vissière-Guérinet
 * @author Marc Sordi
 */
public interface IPublisherContract {

    /**
     * Publish an {@link ISubscribable} event
     * @param event {@link ISubscribable} event to publish
     */
    void publish(ISubscribable event);

    /**
     * Publish in batch a list of {@link ISubscribable} events
     *
     * <br/><br/><b>!!!!! Experimental feature for test only at the moment</b>
     */
    void publish(List<? extends ISubscribable> events);

    /**
     * Publish an {@link ISubscribable} event
     * @param event {@link ISubscribable} event to publish
     * @param priority event priority
     */
    void publish(ISubscribable event, int priority);

    /**
     * Publish in batch a list of {@link ISubscribable} events with specified priority
     *
     * <br/><br/><b>!!!!! Experimental feature for test only at the moment</b>
     */
    void publish(List<? extends ISubscribable> events, int priority);

    /**
     * Publish an {@link IPollable} event
     * @param event {@link IPollable} event to publish
     */
    void publish(IPollable event);

    /**
     * Publish an {@link IPollable} event
     * @param event {@link IPollable} event to publish
     * @param purgeQueue true to purge queue before publishing event. Useful in tests.
     */
    void publish(IPollable event, boolean purgeQueue);

    /**
     * Publish an {@link IPollable} event
     * @param event {@link IPollable} event to publish
     * @param priority event priority
     */
    void publish(IPollable event, int priority);

    /**
     * Publish an {@link IPollable} event
     * @param event {@link IPollable} event to publish
     * @param priority event priority
     * @param purgeQueue true to purge queue before publishing event. Useful in tests.
     */
    void publish(IPollable event, int priority, boolean purgeQueue);

    /**
     * Purge related queue. Useful for testing purpose before publishing. Purge can be done as well with
     * {@link #publish(IPollable, boolean)} or {@link #publish(IPollable, int, boolean)}
     * @param eventType {@link IPollable} event type
     */
    void purgeQueue(Class<? extends IPollable> eventType);

    /**
     * Broadcast message to specified exchange optionally creating a binded queue.
     */
    void broadcast(String exchangeName, Optional<String> queueName, int priority, Object message);
}
