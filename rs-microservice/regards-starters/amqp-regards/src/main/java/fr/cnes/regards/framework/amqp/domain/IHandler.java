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
 * @param <M> Type of message you are handling
 *
 * Interface identifying classes that can handle message from the broker
 * @author svissier
 */
public interface IHandler<M> {

    /**
     * Logger instance
     */
    Logger LOGGER = LoggerFactory.getLogger(IHandler.class);

    /**
     * @deprecated User {@link #handleAndLog(String, Object)} instead.
     * @param wrapper
     */
    @Deprecated
    default void handleAndLog(TenantWrapper<M> wrapper) {
        LOGGER.debug("Received {}, From {}", wrapper.getContent().getClass().getSimpleName(), wrapper.getTenant());
        LOGGER.trace("Event received: {}", wrapper.getContent().toString());
        handle(wrapper);
    }

    default void handleAndLog(String tenant, M message) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Received {}, From {}", message.getClass().getSimpleName(), tenant);
            LOGGER.trace("Event received: {}", message.toString());
        }
        handle(tenant, message);
    }

    /**
     * @deprecated Use {@link #handle(String, Object)} instead.
     */
    @Deprecated
    default void handle(TenantWrapper<M> wrapper) {
        throw new UnsupportedOperationException("This method is deprecated");
    }

    default void handle(String tenant, M message) {
        // Default implementation for compatibility
        // This interface will be required in next version
        handle(TenantWrapper.build(message, tenant));
    }

    @SuppressWarnings("unchecked")
    default Class<? extends IHandler<M>> getType() {
        return (Class<? extends IHandler<M>>) this.getClass();
    }
}
