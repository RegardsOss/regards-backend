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
package fr.cnes.regards.framework.amqp.batch;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;

/**
 * @param <T> Type of Event you are handling
 *
 * Interface identifying classes that can handle message from the broker
 * @author svissier
 */
public interface IBatchHandler<T> extends IHandler<T> {

    /**
     * Logger instance
     */
    Logger LOGGER = LoggerFactory.getLogger(IBatchHandler.class);

    default void handleBatchAndLog(String tenant, List<T> messages) {
        if (LOGGER.isTraceEnabled()) {
            for (T message : messages) {
                LOGGER.trace("Received {}, From {}", message.getClass().getSimpleName(), tenant);
                LOGGER.trace("Event received: {}", message.toString());
            }
        }
        handleBatch(tenant, messages);
    }

    void handleBatch(String tenant, List<T> messages);

    /**
     * @return batch size. Look at {@link SimpleMessageListenerContainer#setBatchSize(int)} for better understanding.
     */
    default int getBatchSize() {
        return 1000;
    }

    /**
     * @return receive timeout. Look at {@link SimpleMessageListenerContainer#setReceiveTimeout(long)} for better understanding.
     */
    default long getReceiveTimeout() {
        return 1000;
    }

    @Override
    default void handle(TenantWrapper<T> wrapper) {
        throw new UnsupportedOperationException("Should never be called by the container");
    }
}
