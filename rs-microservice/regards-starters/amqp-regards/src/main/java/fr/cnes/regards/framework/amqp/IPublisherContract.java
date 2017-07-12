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

import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.ISubscribable;

/**
 * {@link IPublisherContract} allows to publish {@link ISubscribable} or {@link IPollable} events. This interface
 * represents the common publisher contract whether we are in a multitenant or an instance context.
 *
 * @author Sylvain Vissière-Guérinet
 * @author Marc Sordi
 */
public interface IPublisherContract {

    /**
     * Publish an {@link ISubscribable} event
     *
     * @param pEvent
     *            {@link ISubscribable} event to publish
     */
    void publish(ISubscribable pEvent);

    /**
     * Publish an {@link ISubscribable} event
     *
     * @param pEvent
     *            {@link ISubscribable} event to publish
     * @param pPriority
     *            event priority
     */
    void publish(ISubscribable pEvent, int pPriority);

    /**
     * Publish an {@link IPollable} event
     *
     * @param pEvent
     *            {@link IPollable} event to publish
     */
    void publish(IPollable pEvent);

    /**
     * Publish an {@link IPollable} event
     *
     * @param pEvent
     *            {@link IPollable} event to publish
     * @param pPriority
     *            event priority
     */
    void publish(IPollable pEvent, int pPriority);
}
