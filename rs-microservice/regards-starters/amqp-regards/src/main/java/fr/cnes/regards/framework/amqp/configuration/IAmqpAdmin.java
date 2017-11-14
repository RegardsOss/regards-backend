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
package fr.cnes.regards.framework.amqp.configuration;

import java.util.Optional;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;

/**
 * @author Marc Sordi
 *
 */
public interface IAmqpAdmin {

    /**
     * Declare an exchange for each event so we use its name to instantiate it.
     *
     * @param eventType event type
     * @param workerMode {@link WorkerMode}
     * @param target {@link Target}
     * @return a new {@link Exchange} related to current tenant and event
     */
    public Exchange declareExchange(Class<?> eventType, WorkerMode workerMode, Target target);

    /**
     * Declare a queue that can handle priority
     *
     * @param tenant tenant for which the queue is created
     * @param eventType event type inheriting {@link IPollable} or {@link ISubscribable}
     * @param workerMode worker mode
     * @param target targer
     * @param handler optional event handler extending {@link IHandler}
     * @return declared {@link Queue}
     */
    public Queue declareQueue(String tenant, Class<?> eventType, WorkerMode workerMode, Target target,
            Optional<? extends IHandler<?>> handler);

    /**
     * Declare binding to link {@link Queue} and {@link Exchange} with an optional routing key
     *
     * @param queue {@link Queue} to bind
     * @param exchange {@link Exchange} to bind
     * @param workerMode {@link WorkerMode} to compute routing key
     * @return {@link Binding}
     */
    public Binding declareBinding(Queue queue, Exchange exchange, WorkerMode workerMode);

    /**
     * Routing key build according to {@link WorkerMode}.
     *
     * @param queue queue
     * @param workerMode worker mode
     * @return routing key
     */
    public String getRoutingKey(Optional<Queue> queue, WorkerMode workerMode);
}
