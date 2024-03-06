/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.rabbitmq.client.Channel;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.batch.dto.BatchMessage;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.RetryProperties;
import fr.cnes.regards.framework.amqp.exception.MaxRetriesReachedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.AmqpHeaders;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Service responsible for retrying a batch of AMQP messages that have failed due to an unexpected exception. The
 * messages are published to an 'x-delayed-type' exchange in a single transaction and re-routed to their original
 * queues after a configurable amount of time.</p>
 * <p>It should be noted that the number of retries is limited. If the limit is reached, the batch will be discarded
 * and sent to the dead-letter exchange (DLX).</p>
 *
 * @author Iliana Ghazali
 **/
public class RetryBatchMessageHandler {

    public static final String X_RETRY_HEADER = "x-retries";

    // CONSTANTS

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryBatchMessageHandler.class);

    private final String microserviceName;

    // SERVICES

    private final IPublisher publisher;

    private final BatchMessageErrorHandler batchMessageErrorHandler;

    private final IAmqpAdmin amqpAdmin;

    private final RetryProperties retryProperties;

    public RetryBatchMessageHandler(IPublisher publisher,
                                    IAmqpAdmin amqpAdmin,
                                    BatchMessageErrorHandler batchMessageErrorHandler,
                                    RetryProperties retryProperties,
                                    String microserviceName) {
        this.publisher = publisher;
        this.amqpAdmin = amqpAdmin;
        this.batchMessageErrorHandler = batchMessageErrorHandler;
        this.retryProperties = retryProperties;
        this.microserviceName = microserviceName;
    }

    /**
     * Retry a batch of AMQP messages.
     * <p>Each failed AMQP message is either sent to a retry exchange or re-routed to a dead-letter exchange if the
     * number of retries has been exhausted. In case of retry, the message is sent to a 'delayed-type' exchange and
     * retained during a configurable amount of time, passed this delay the message will be sent back to its original
     * queue.</p>
     *
     * @param tenant        project at the origin of the messages
     * @param validMessages AMQP messages that have been validated beforehand
     * @param channel       AMQP channel
     * @param exception     unexpected exception to keep track of the failure
     */
    public void handleBatchMessageRetry(String tenant,
                                        List<BatchMessage> validMessages,
                                        Channel channel,
                                        Exception exception) {
        List<BatchMessage> deadMessages = new ArrayList<>(validMessages.size());
        for (BatchMessage batchMessage : validMessages) {
            Message messageOrigin = batchMessage.getOrigin();
            MessageProperties messageProperties = messageOrigin.getMessageProperties();
            int retryValueHeader = getRetryValueHeader(messageProperties);
            int maxRetries = retryProperties.getMaxRetries();
            // Check if message is retryable
            if (retryValueHeader < maxRetries) {
                updateRetryHeaders(messageProperties, retryValueHeader, maxRetries);
                // republish again the message with delay
                publisher.basicPublish(tenant,
                                       amqpAdmin.getRetryExchangeName(),
                                       messageProperties.getConsumerQueue(),
                                       messageOrigin);
            } else {
                deadMessages.add(batchMessage);
            }
        }
        // Handle all dead messages as they cannot be retried anymore
        if (!deadMessages.isEmpty()) {
            batchMessageErrorHandler.discardBatch(tenant,
                                                  deadMessages,
                                                  new MaxRetriesReachedException(String.format(
                                                      "The AMQP messages will not be retried anymore because it they have "
                                                      + "reached the maximum number of retries allowed. "
                                                      + "For more information, refer to the '%s' microservice logs.",
                                                      microserviceName), exception));
        }
    }

    /**
     * Initialize or retrieve the retry header in the AMQP message properties.
     */
    private int getRetryValueHeader(MessageProperties messageProperties) {
        Integer retryValue = messageProperties.getHeader(X_RETRY_HEADER);
        if (retryValue == null) {
            retryValue = 0;
            messageProperties.setHeader(X_RETRY_HEADER, retryValue);
        }
        return retryValue;

    }

    /**
     * Update the retry and delay headers for the next retry iteration.
     */
    private void updateRetryHeaders(MessageProperties messageProperties, int currentRetryValue, int maxRetries) {
        int nextRetryValue = currentRetryValue + 1;
        int nextDelayValue = (int) retryProperties.getDelayAttempts().get(currentRetryValue).toMillis();
        messageProperties.setHeader(X_RETRY_HEADER, nextRetryValue);
        messageProperties.setDelay(nextDelayValue);

        LOGGER.debug("[requestId: {}, correlationId: {}] Retrying message with delay of {}ms (current retry {}, "
                     + "maximum of retries {}).",
                     messageProperties.getHeader(AmqpConstants.REGARDS_REQUEST_ID_HEADER),
                     messageProperties.getHeader(AmqpHeaders.CORRELATION_ID),
                     nextDelayValue,
                     currentRetryValue,
                     maxRetries);
    }

}
