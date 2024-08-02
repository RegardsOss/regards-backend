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
package fr.cnes.regards.framework.amqp.event;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Optional;

/**
 * Default Interface for amqp message to send.
 *
 * @author Sébastien Binda
 **/
public interface IEvent {

    /**
     * Define the default amqp property correlation_id when sending message to rabbitmq exchange.
     */
    @JsonIgnore
    default Optional<String> getMessageCorrelationId() {
        return Optional.empty();
    }

    /**
     * Define the default amqp property origin request app_id when sending message to rabbitmq exchange.
     * <p>
     * Used to set the originAppId in the body of the optional duplicated message sent to notifier.
     * All AMQP messages sent by REGARDS can be sent twice (by configuration).
     */
    @JsonIgnore
    default Optional<String> getOriginRequestAppId() {
        return Optional.empty();
    }

    /**
     * Define the default amqp property origin priority when sending message to rabbitmq exchange.
     * <p>
     * Used to set the origin request priority in the body of the optional duplicated message sent to notifier.
     * All AMQP messages sent by REGARDS can be sent twice (by configuration).
     */
    @JsonIgnore
    default Optional<Integer> getOriginRequestPriority() {
        return Optional.of(Integer.valueOf(1));
    }

}
