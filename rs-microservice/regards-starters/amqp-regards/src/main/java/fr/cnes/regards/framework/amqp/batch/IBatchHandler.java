/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.validation.Errors;

import java.util.List;
import java.util.Optional;

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

    default void handleBatchAndLog(List<M> messages, List<Message> rawMessages) {
        if (LOGGER.isTraceEnabled()) {
            for (M message : messages) {
                LOGGER.trace("Received {}", message.getClass().getSimpleName());
                LOGGER.trace("Event received: {}", message.toString());
            }
        }
        handleBatchWithRaw(messages, rawMessages);
    }

    /**
     * @return generic type, allows to inject converted type into message headers for GSON converted messages
     */
    default Class<M> getMType() {
        return null;
    }

    /**
     * This method is called for each message that cannot be converted
     * by the selected JSON converter before message gets routed to DLQ.<br/>
     * So system may manage business behavior programmatically.
     *
     * If not, return <code>false</code> so the default behavior will be applied (e.g. project or instance notification).
     * @param message the message
     * @param errorMessage the message conversion error
     * @return <code>true</code> or <code>false</code> to respectively enable or disable the sending of default notifications.
     */
    default boolean handleConversionError(Message message, String errorMessage) {
        return false;
    }

    /**
     * This method is called for each message<br/>
     * Invalid message is negatively acknowledged (and so routed to DLQ.)<br/>
     * Valid message is processed using {@link #handleBatch(List)} method.<br/>
     * If an error occurs during batch processing, all valid messages are negatively acknowledged
     * even if only single message causes the error.<br/>
     * Consequently, validation must be as efficient as possible to avoid this behavior
     * and guarantee the processing of valid messages as much as possible.
     *
     * @param message messages to manage
     * @return list of errors. If this list is null or empty, the message is considered valid.
     */
    Errors validate(M message);

    /**
     * This method is called once for each tenant with messages in the current batch.<br/>
     * Indeed, a batch is composed of the n first messages in the queue without consideration of the tenant.
     * So the batch listener dispatches them by tenant under the hood to make a contextual call per tenant.
     * @param messages messages to manage
     */
    default void handleBatchWithRaw(List<M> messages, List<Message> rawMessages) {
        handleBatch(messages);
    }

    void handleBatch(List<M> messages);

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

    /**
     * @return <code>true</code> to enable a dedicated DLQ for this handler. In this case, error messages will be routed to this DLQ.
     * By default, message are routed to global system DLQ.
     */
    default boolean isDedicatedDLQEnabled() {
        return false;
    }

    /**
     * @return dlq routing queue value.
     */
    default Optional<String> getDLQRoutingKey() { return Optional.empty(); }

    @Override
    default void handle(TenantWrapper<M> wrapper) {
        throw new UnsupportedOperationException("Should never be called by the container");
    }
}
