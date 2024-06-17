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

import fr.cnes.regards.framework.amqp.batch.dto.BatchMessage;
import fr.cnes.regards.framework.amqp.batch.dto.ResponseMessage;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.validation.Errors;

import java.util.List;
import java.util.Optional;

/**
 * Interface identifying classes that can handle message from the broker
 *
 * @param <M> Type of messages you are handling
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
                LOGGER.trace("Event received: {}", message);
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
     * This method is called for each message that could not be deserialized from an AMQP message to an Object.
     * Each handler has the ability to return optionally an AMQP error message, which will be automatically
     * republished to the specified exchange.
     *
     * @param message      raw AMQP message
     * @param errorMessage conversion error
     * @return optional AMQP response message, empty by default.
     */
    default ResponseMessage<? extends ISubscribable> buildDeniedResponseForNotConvertedMessage(Message message,
                                                                                               String errorMessage) {
        return ResponseMessage.buildEmptyResponse();
    }

    /**
     * This method is called for each message that has been invalidated by {@link this#validate(Object)}.
     * Each handler has the ability to return optionally an AMQP error message, which will be automatically
     * republished to the specified exchange.
     *
     * @param batchMessage the invalid batch message
     * @param errorMessage description of the error
     * @return optional AMQP response message, empty by default.
     */
    default ResponseMessage<? extends ISubscribable> buildDeniedResponseForInvalidMessage(BatchMessage batchMessage,
                                                                                          String errorMessage) {
        return ResponseMessage.buildEmptyResponse();
    }

    /**
     * This method is called for each message<br/>
     * An invalid message is acknowledged and trigger a DENIED message (if configured see
     * {@link #buildDeniedResponseForInvalidMessage})<br/>
     * A valid message is processed using {@link #handleBatch(List)} method.<br/>
     * If an error occurs during a batch processing, valid messages are processed nominally and invalid messages are
     * DENIED.
     *
     * @param message messages to manage
     * @return list of errors. If this list is null or empty, the message is considered valid.
     */
    Errors validate(M message);

    /**
     * This method is called once for each tenant with messages in the current batch.<br/>
     * Indeed, a batch is composed of the n first messages in the queue without consideration of the tenant.
     * So the batch listener dispatches them by tenant under the hood to make a contextual call per tenant.
     *
     * <b>CAUTION</b> : This method is not transactional. If you need a transaction, create it in your method
     * implementation.
     *
     * @param messages messages to manage
     */
    default void handleBatchWithRaw(List<M> messages, List<Message> rawMessages) {
        handleBatch(messages);
    }

    /**
     * <b>CAUTION</b> : This method is not transactional. If you need a transaction, create it in your method
     * * implementation.
     */
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
     * @return <code>true</code> by default to enable a dedicated DLQ for this handler. In this case, error messages
     * will be routed to this DLQ. If false, message are routed to global system DLQ.
     */
    default boolean isDedicatedDLQEnabled() {
        return true;
    }

    /**
     * @return dlq routing queue value.
     */
    default Optional<String> getDLQRoutingKey() {
        return Optional.empty();
    }

    @Override
    default void handle(TenantWrapper<M> wrapper) {
        throw new UnsupportedOperationException("Should never be called by the container");
    }
}
