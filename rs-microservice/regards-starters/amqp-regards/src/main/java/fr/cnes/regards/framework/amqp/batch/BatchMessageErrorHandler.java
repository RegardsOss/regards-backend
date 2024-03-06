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

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.batch.dto.BatchErrorResponse;
import fr.cnes.regards.framework.amqp.batch.dto.BatchMessage;
import fr.cnes.regards.framework.amqp.batch.dto.BatchMessageErrorType;
import fr.cnes.regards.framework.amqp.batch.dto.ResponseMessage;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationDtoBuilder;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.cnes.regards.framework.amqp.batch.dto.BatchMessageErrorType.UNEXPECTED_BATCH_FAILURE;

/**
 * Error handler associated to the {@link RabbitBatchMessageListener}. This service handles any errors that
 * may have occurred while processing a batch of AMQP messages either by discarding or denying them.
 *
 * @author Iliana Ghazali
 **/
public class BatchMessageErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchMessageErrorHandler.class);

    private final String microserviceName;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IInstancePublisher instancePublisher;

    private final IPublisher publisher;

    private final IBatchHandler<?> batchHandler;

    private final RepublishErrorBatchMessageRecover republishErrorRecover;

    public BatchMessageErrorHandler(IAmqpAdmin amqpAdmin,
                                    RabbitTemplate rabbitTemplate,
                                    IRuntimeTenantResolver runtimeTenantResolver,
                                    IInstancePublisher instancePublisher,
                                    IPublisher publisher,
                                    IBatchHandler<?> batchHandler,
                                    String microserviceName) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.instancePublisher = instancePublisher;
        this.publisher = publisher;
        this.batchHandler = batchHandler;
        this.microserviceName = microserviceName;
        this.republishErrorRecover = new RepublishErrorBatchMessageRecover(rabbitTemplate,
                                                                           publisher,
                                                                           amqpAdmin,
                                                                           batchHandler.isDedicatedDLQEnabled());
    }

    /**
     * Reject a batch of messages by republishing them to the dead-letter exchange.
     *
     * @param tenant    project at the origin of the messages
     * @param messages  AMQP messages to discard
     * @param exception exception to attach to each messages in error.
     */
    public void discardBatch(String tenant, List<BatchMessage> messages, Exception exception) {
        // Log error
        String errorMessage = String.format("[%s] %d messages are routed to DLQ for handler '%s'. Cause: '%s'.",
                                            UNEXPECTED_BATCH_FAILURE,
                                            messages.size(),
                                            batchHandler.getClass().getName(),
                                            getExceptionErrorMessage(exception));
        LOGGER.error(errorMessage, exception);

        // Send app notifications and error responses
        BatchErrorResponse batchErrorResponse = new BatchErrorResponse();
        batchErrorResponse.appendError(buildErrorNotification(UNEXPECTED_BATCH_FAILURE.getLabel(),
                                                              errorMessage,
                                                              microserviceName), ResponseMessage.buildEmptyResponse());
        sendBatchErrors(batchErrorResponse, tenant);

        // Reject messages by republishing them to the DLX
        republishErrorRecover.handleBatchRecover(messages, exception);
    }

    /**
     * Deny a message that could not pass the conversion nor the validation phase and build a denied response indicating
     * the cause of the refusal.
     *
     * @param message               message in error
     * @param exception             exception which causes the refusal of this message
     * @param batchMessageErrorType type of error, refer to {@link BatchMessageErrorType}
     */
    public void handleDeniedMessage(BatchMessage message,
                                    BatchErrorResponse batchErrorResponse,
                                    Exception exception,
                                    BatchMessageErrorType batchMessageErrorType) {
        // Log error
        Message messageOrigin = message.getOrigin();
        String errorMsg = String.format("""
                                            Errors detected while processing AMQP message with id %d and correlation id '%s'.
                                            --> Error type: '%s'
                                            --> Error message: '%s'
                                            --> Received AMQP Message: '%s'""",
                                        messageOrigin.getMessageProperties().getDeliveryTag(),
                                        messageOrigin.getMessageProperties().getCorrelationId(),
                                        batchMessageErrorType,
                                        getExceptionErrorMessage(exception),
                                        message);

        LOGGER.error(errorMsg, exception);

        // Build error notification
        NotificationEvent notificationError = buildErrorNotification(batchMessageErrorType.getLabel(),
                                                                     errorMsg,
                                                                     microserviceName);

        // Build AMQP response if any
        ResponseMessage<? extends ISubscribable> responseMessage = switch (batchMessageErrorType) {
            case NOT_CONVERTED_MESSAGE ->
                batchHandler.buildDeniedResponseForNotConvertedMessage(messageOrigin, errorMsg);
            case INVALID_MESSAGE -> batchHandler.buildDeniedResponseForInvalidMessage(message, errorMsg);
            default -> ResponseMessage.buildEmptyResponse();
        };

        // Add error to batch
        batchErrorResponse.appendError(notificationError, responseMessage);
    }

    /**
     * Handle messages if the tenant currently processed is inactive. All the related messages will be ignored.
     *
     * @param messages raw AMQP messages grouped by tenant.
     * @param tenant   project at the origin of the messages.
     */
    public void handleInactiveTenant(List<Message> messages, String tenant) {
        LOGGER.warn("[AMQP MESSAGE - INVALID TENANT] Current microservice is not configured for tenant '{}'. {} "
                    + "messages ignored.", tenant, messages.size());
        // Do not send to DLQ, it is possible that a service is not available for a given tenant. In this case,
        // just ignore messages.
    }

    /**
     * Publish errors collected in the {@link BatchMessageErrorType} in a 2-step process:
     * <ul>
     * <li>Send a single notification error to project administrators or instance administrators, in case no tenant
     * has been provided.</li>
     * <li>Send all response messages eventually built in a unique transaction.</li>
     * </ul>
     *
     * @param batchErrorResponse object to collect errors during the processing of a batch.
     * @param tenant             project at the origin of the messages.
     */
    public void sendBatchErrors(BatchErrorResponse batchErrorResponse, String tenant) {
        // Send notification errors
        NotificationEvent globalNotificationError = buildOneNotificationEventFromErrors(microserviceName,
                                                                                        batchErrorResponse.getNotificationErrors());
        // Notify project
        if (tenant != null) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                // send only one notification for errors in the batch
                publisher.publish(globalNotificationError);
                // send response events
                publisher.publish(batchErrorResponse.getResponseMessages()
                                                    .stream()
                                                    .map(ResponseMessage::getResponsePayload)
                                                    .toList());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        } else {
            // Notify instance
            instancePublisher.publish(globalNotificationError);
        }
    }

    private NotificationEvent buildErrorNotification(String title, String errorMessage, String microserviceName) {
        NotificationDtoBuilder builder = new NotificationDtoBuilder(errorMessage,
                                                                    title,
                                                                    NotificationLevel.ERROR,
                                                                    microserviceName);
        Set<String> roles = new HashSet<>(Collections.singletonList(DefaultRole.EXPLOIT.toString()));
        return NotificationEvent.build(builder.toRoles(roles));

    }

    private NotificationEvent buildOneNotificationEventFromErrors(String microserviceName,
                                                                  List<NotificationEvent> notificationErrors) {
        return buildErrorNotification("AMQP batch processing failure.",
                                      String.format(
                                          "Got a total of %d errors with the following types, for more information on "
                                          + "the errors refer to the '%s' microservice logs : ['%s].",
                                          notificationErrors.size(),
                                          microserviceName,
                                          notificationErrors.stream()
                                                            .collect(Collectors.groupingBy(notificationEvent -> notificationEvent.getNotification()
                                                                                                                                 .getTitle(),
                                                                                           Collectors.counting()))
                                                            .entrySet()
                                                            .stream()
                                                            .map(e -> String.format("'%s' : %s errors",
                                                                                    e.getKey(),
                                                                                    e.getValue()))
                                                            .collect(Collectors.joining(", "))),
                                      microserviceName);
    }

    private String getExceptionErrorMessage(Exception exception) {
        String errorMsg;
        Throwable cause = exception.getCause();
        if (cause != null) {
            if (cause instanceof InvocationTargetException targetException) {
                errorMsg = targetException.getMessage();
            } else {
                errorMsg = cause.getMessage();
            }
        } else {
            errorMsg = exception.getMessage();
        }
        return errorMsg;
    }

}
