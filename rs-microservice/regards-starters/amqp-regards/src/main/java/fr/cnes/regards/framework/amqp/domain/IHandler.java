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
package fr.cnes.regards.framework.amqp.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <T> Type of Event you are handling
 *
 * Interface identifying classes that can handle message from the broker
 * @author svissier
 */
public interface IHandler<T> {

    /**
     * Logger instance
     */
    Logger LOGGER = LoggerFactory.getLogger(IHandler.class);

    default void handleAndLog(TenantWrapper<T> wrapper) {
        LOGGER.debug("Received {}, From {}", wrapper.getContent().getClass().getSimpleName(), wrapper.getTenant());
        LOGGER.trace("Event received: {}", wrapper.getContent().toString());
        handle(wrapper);
    }

    void handle(TenantWrapper<T> wrapper);

    @SuppressWarnings("unchecked")
    default Class<? extends IHandler<T>> getType() {
        return (Class<? extends IHandler<T>>) this.getClass();
    }
}
