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
package fr.cnes.regards.framework.amqp.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 * This annotation is required for all AMQP events.
 *
 * <h1>For {@link ISubscribable} events:</h1>
 * <br/>
 * <ul>
 * <li>{@link WorkerMode#BROADCAST} / {@link Target#ALL} : event will
 * be received by ALL handlers of ALL microservice instances.</li>
 * <li>{@link WorkerMode#BROADCAST} / {@link Target#MICROSERVICE} : event will
 * be received by ALL handlers of a ALL microservice instances which type is the same as the PUBLISHING
 * one.</li>
 * <li>{@link WorkerMode#BROADCAST} / {@link Target#ONE_PER_MICROSERVICE_TYPE} : event will
 * be received by ALL handlers of a ONE microservice instance PER microservice instance.</li>
 * <li>{@link WorkerMode#UNICAST} / {@link Target#MICROSERVICE} : event will
 * be received by a SINGLE handler of a SINGLE microservice instance which type is the same as the PUBLISHING
 * one.</li>
 * <li>{@link WorkerMode#UNICAST} / {@link Target#ALL} : event will
 * be received by a SINGLE handler of a SINGLE microservice instance WHATEVER the microservice type.</li>
 * </ul>
 *
 * <h1>For {@link IPollable} events:</h1>
 * <br/>
 * <ul>
 * <li>{@link WorkerMode#UNICAST} / {@link Target#MICROSERVICE} (default behaviour) : event can be polled ONCE by
 * the FIRST microservice instance which type is the same as the PUBLISHING
 * one.</li>
 * <li>{@link WorkerMode#UNICAST} / {@link Target#ALL} : event can be polled ONCE by
 * the FIRST microservice instance WHATEVER the microservice type.</li>
 * </ul>
 *
 * @author Marc Sordi
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.TYPE)
public @interface Event {

    /**
     * With {@link ISubscribable} events, all {@link Target} are usable.<br/>
     * With {@link IPollable} events, only {@link Target#ALL} or {@link Target#MICROSERVICE} are.<br/>
     * Look at {@link Event} javadoc for usage.
     *
     * @return event {@link Target}
     */
    Target target();

    /**
     * This mode is only used for {@link ISubscribable} event.<br/>
     * Look at {@link Event} javadoc for usage.
     *
     * @return event {@link WorkerMode}
     */
    WorkerMode mode() default WorkerMode.BROADCAST;
}
