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
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;

/**
 * @param <M> Type of messages you are handling
 *
 * Interface identifying classes that can handle message from the broker
 * @author svissier
 */
public interface IBatchHandler<M> extends IHandler<M> {

    /**
     * Logger instance
     */
    Logger LOGGER = LoggerFactory.getLogger(IBatchHandler.class);

    default void handleBatchAndLog(String tenant, List<M> messages) {
        if (LOGGER.isTraceEnabled()) {
            for (M message : messages) {
                LOGGER.trace("Received {}, From {}", message.getClass().getSimpleName(), tenant);
                LOGGER.trace("Event received: {}", message.toString());
            }
        }
        handleBatch(tenant, messages);
    }

    /**
     * This method is called for each message that cannot be converted
     * by the selected JSON converter before message gets routed to DLQ.<br/>
     * So system may manage business behavior programmatically.
     *
     * If not, return <code>false</code> so the default behavior will be applied (e.g. project or instance notification).
     * @param tenant related message tenant
     * @param message the message
     * @param errorMessage the message conversion error
     * @return <code>true</code> or <code>false</code> to respectively enable or disable the sending of default notifications.
     */
    default boolean handleConversionError(String tenant, Message message, String errorMessage) {
        return true;
    }

    /**
     * This method is called for each message<br/>
     * Invalid message is negatively acknowledged (and so routed to DLQ.)<br/>
     * Valid message is processed using {@link #handleBatch(String, List)} method.<br/>
     * If an error occurs, these valid messages are re-queued for re-processing.<br/>
     * A valid message is delivered over and over again and can lead to an infinite loop if
     * this message can never be processed so validate method has to be as efficient as possible.
     * @param tenant related message tenant
     * @param message messages to manage
     * @return <code>true</code> is message is valid.
     */
    boolean validate(String tenant, M message);

    /**
     * This method is called once for each tenant with messages in the current batch.<br/>
     * Indeed, a batch is composed of the n first messages in the queue without consideration of the tenant.
     * So the batch listener dispatches them by tenant under the hood to make a contextual call per tenant.
     * @param tenant related message tenant
     * @param messages messages to manage
     */
    void handleBatch(String tenant, List<M> messages);

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
    default void handle(TenantWrapper<M> wrapper) {
        throw new UnsupportedOperationException("Should never be called by the container");
    }
}
