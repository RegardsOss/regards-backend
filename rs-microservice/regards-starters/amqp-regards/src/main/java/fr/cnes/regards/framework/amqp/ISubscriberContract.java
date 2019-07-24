/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
     */
    <T extends ISubscribable> void unsubscribeFrom(Class<T> eventType);

    /**
     * Purge related queues (for all tenant virtual hosts). Useful for testing purpose before publishing events. Purge
     * can be done as well using {@link #subscribeTo(Class, IHandler, boolean)}
     * @param eventType {@link ISubscribable} event type
     * @param handlerType {@link IHandler} type
     */
    <E extends ISubscribable> void purgeQueue(Class<E> eventType, Class<? extends IHandler<E>> handlerType);
}
