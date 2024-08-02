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
package fr.cnes.regards.framework.amqp.batch;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.rabbitmq.client.Channel;
import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.batch.dto.BatchErrorResponse;
import fr.cnes.regards.framework.amqp.batch.dto.BatchMessage;
import fr.cnes.regards.framework.amqp.batch.dto.BatchMessageErrorType;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.RetryProperties;
import fr.cnes.regards.framework.amqp.converter.Gson2JsonMessageConverter;
import fr.cnes.regards.framework.amqp.exception.InvalidMessageException;
import fr.cnes.regards.framework.amqp.exception.MissingTenantException;
import fr.cnes.regards.framework.amqp.exception.UnprocessableBatchException;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareBatchMessageListener;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.MethodInvoker;
import org.springframework.validation.Errors;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Batch listener to handle AMQP messages received from multiple tenants.
 */
public class RabbitBatchMessageListener implements ChannelAwareBatchMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitBatchMessageListener.class);

    // CONSTANTS

    // method names for reflection

    private static final String VALIDATE_SINGLE_METHOD_NAME = "validate";

    private static final String HANDLE_METHOD_NAME = "handleBatchAndLog";

    // SERVICES

    private final MessageConverter messageConverter;

    private final IBatchHandler<?> batchHandler;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ITenantResolver tenantResolver;

    private final BatchMessageErrorHandler batchMessageErrorHandler;

    private final RetryBatchMessageHandler retryBatchMessageHandler;

    private final TransactionTemplate transactionTemplate;

    public RabbitBatchMessageListener(IAmqpAdmin amqpAdmin,
                                      String microserviceName,
                                      IInstancePublisher instancePublisher,
                                      IPublisher publisher,
                                      IRuntimeTenantResolver runtimeTenantResolver,
                                      ITenantResolver tenantResolver,
                                      MessageConverter messageConverter,
                                      IBatchHandler<?> batchHandler,
                                      RabbitTemplate rabbitTemplate,
                                      TransactionTemplate transactionTemplate,
                                      RetryProperties retryProperties) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.tenantResolver = tenantResolver;
        this.messageConverter = messageConverter;
        this.batchHandler = batchHandler;
        this.transactionTemplate = transactionTemplate;
        this.batchMessageErrorHandler = new BatchMessageErrorHandler(amqpAdmin,
                                                                     rabbitTemplate,
                                                                     runtimeTenantResolver,
                                                                     instancePublisher,
                                                                     publisher,
                                                                     batchHandler,
                                                                     microserviceName);
        this.retryBatchMessageHandler = new RetryBatchMessageHandler(publisher,
                                                                     amqpAdmin,
                                                                     batchMessageErrorHandler,
                                                                     retryProperties,
                                                                     microserviceName);
    }

    @Override
    public void onMessageBatch(List<Message> messages, Channel channel) {
        // Handle messages by tenant
        try {
            Multimap<String, Message> messagesByTenant = mapMessagesByTenant(messages);
            for (String tenant : messagesByTenant.keySet()) {
                Collection<Message> mappedMessages = messagesByTenant.get(tenant);
                // Check if tenant is active
                if (tenantResolver.getAllActiveTenants().contains(tenant)) {
                    // Convert messages to java objects
                    List<BatchMessage> convertedMessages = convertToBatchMessages(mappedMessages, tenant);
                    if (!convertedMessages.isEmpty()) {
                        // Verify that messages are valid
                        List<BatchMessage> validMessages = getValidMessages(convertedMessages, tenant);
                        if (!validMessages.isEmpty()) {
                            processValidMessages(tenant, validMessages);
                        }
                    }
                } else {
                    batchMessageErrorHandler.handleInactiveTenant(messages, tenant);
                }
            }
        } finally {
            // in any cases, acknowledge incoming messages from rabbitmq listener as acknowledge mode is manual
            acknowledgeMessages(messages, channel);
        }
    }

    /**
     * Map {@link Message}s received by tenant.
     *
     * @param messages raw AMQP messages received from multiple tenants.
     * @return Map of messages grouped by tenant.
     */
    private Multimap<String, Message> mapMessagesByTenant(List<Message> messages) {
        // Map of messages by tenant
        Multimap<String, Message> messagesByTenant = ArrayListMultimap.create();
        BatchErrorResponse batchErrorResponse = new BatchErrorResponse();
        for (Message message : messages) {
            setDefaultHeaders(message);
            String tenant = message.getMessageProperties().getHeader(AmqpConstants.REGARDS_TENANT_HEADER);
            // Check tenant
            if (tenant == null || tenant.isEmpty()) {
                batchMessageErrorHandler.handleDeniedMessage(BatchMessage.buildNotConvertedBatchMessage(message),
                                                             batchErrorResponse,
                                                             new MissingTenantException(
                                                                 "Tenant is not present on batch message properties, message "
                                                                 + "cannot be further processed!"),
                                                             BatchMessageErrorType.MISSING_TENANT);
            } else {
                messagesByTenant.put(tenant, message);
            }
        }
        if (batchErrorResponse.hasErrors()) {
            batchMessageErrorHandler.sendBatchErrors(batchErrorResponse, null);
        }
        return messagesByTenant;
    }

    /**
     * Convert {@link Message}s to {@link BatchMessage}s. Not converted messages will be discarded and may lead to the publishing of
     * response messages on a queue configured by the
     * {@link IBatchHandler#buildDeniedResponseForNotConvertedMessage(Message, String) (BatchMessage, String)} method.
     *
     * @param messages raw AMQP messages to convert to java objects. The converter used depends on the parameters
     *                 provided by the AMQP message properties.
     * @param tenant   project at the origin of the messages.
     * @return wrapper that contains the original raw message and the corresponding converted object.
     */
    private List<BatchMessage> convertToBatchMessages(Collection<Message> messages, String tenant) {
        // Map of messages by tenant
        List<BatchMessage> convertedMessages = new ArrayList<>(messages.size());
        BatchErrorResponse batchErrorResponse = new BatchErrorResponse();
        for (Message message : messages) {
            try {
                // Convert message
                BatchMessage batchMessage = BatchMessage.buildConvertedBatchMessage(message,
                                                                                    messageConverter.fromMessage(message));
                convertedMessages.add(batchMessage);
            } catch (MessageConversionException mce) {
                batchMessageErrorHandler.handleDeniedMessage(BatchMessage.buildNotConvertedBatchMessage(message),

                                                             batchErrorResponse,
                                                             mce,
                                                             BatchMessageErrorType.NOT_CONVERTED_MESSAGE);
            }
        }
        if (batchErrorResponse.hasErrors()) {
            executeInTransaction(() -> batchMessageErrorHandler.sendBatchErrors(batchErrorResponse, tenant), tenant);
        }

        return convertedMessages;
    }

    /**
     * Return valid messages from given ones. Invalid messages will be discarded and may lead to the publishing of
     * response messages on a queue configured by the
     * {@link IBatchHandler#buildDeniedResponseForInvalidMessage(BatchMessage, String)} method.
     *
     * @param convertedMessages messages to validate.
     * @param tenant            project at the origin of the messages.
     * @return only valid {@link BatchMessage}s.
     */
    private List<BatchMessage> getValidMessages(Collection<BatchMessage> convertedMessages, String tenant) {
        List<BatchMessage> validMessages = new ArrayList<>();
        BatchErrorResponse batchErrorResponse = new BatchErrorResponse();
        for (BatchMessage convertedMessage : convertedMessages) {
            // check if message can be processed by the current handler
            String error = handleInvalidMessageFromDLQ(convertedMessage);
            if (error != null) {
                batchMessageErrorHandler.handleDeniedMessage(convertedMessage,
                                                             batchErrorResponse,
                                                             new InvalidMessageException(error),
                                                             BatchMessageErrorType.MESSAGE_ORIGIN_MISMATCH);
            } else {
                try {
                    // validate message
                    Errors errors = invokeValidationMethod(tenant, convertedMessage.getConverted());
                    if (errors != null && errors.hasErrors()) {
                        batchMessageErrorHandler.handleDeniedMessage(convertedMessage,
                                                                     batchErrorResponse,
                                                                     new InvalidMessageException(ErrorTranslator.getErrorsAsString(
                                                                         errors)),
                                                                     BatchMessageErrorType.INVALID_MESSAGE);
                    } else {
                        validMessages.add(convertedMessage);
                    }
                } catch (InvocationTargetException | UnprocessableBatchException e) {
                    batchMessageErrorHandler.handleDeniedMessage(convertedMessage,
                                                                 batchErrorResponse,
                                                                 e,
                                                                 BatchMessageErrorType.UNEXPECTED_VALIDATION_FAILURE);
                }
            }
        }
        if (batchErrorResponse.hasErrors()) {
            executeInTransaction(() -> batchMessageErrorHandler.sendBatchErrors(batchErrorResponse, tenant), tenant);
        }
        return validMessages;
    }

    /**
     * Check into message headers if property x-death-queue exists.
     * Property exists means message comes from DLQ, so check if origin queue match with current IBatchHandler.
     * Return error if message cannot be handled by current handler.
     *
     * @param convertedMessage message to check
     * @return error message if invalid
     */
    private String handleInvalidMessageFromDLQ(BatchMessage convertedMessage) {
        String error = null;
        String originQueueName = convertedMessage.getOrigin()
                                                 .getMessageProperties()
                                                 .getHeader(RepublishErrorBatchMessageRecover.X_DEATH_QUEUE_HEADER);
        boolean isValid = originQueueName == null || batchHandler.getType() == null || originQueueName.endsWith(
            batchHandler.getType().getName());
        if (!isValid) {
            error = String.format("%s (%s) found in message headers mismatch with current message handler %s (type=%s)",
                                  RepublishErrorBatchMessageRecover.X_DEATH_QUEUE_HEADER,
                                  originQueueName,
                                  batchHandler.getClass().getName(),
                                  batchHandler.getType());
        }
        return error;
    }

    /**
     * Invokes the main {@link IBatchHandler} method to process a batch of messages that are grouped by tenant.
     * If an unexpected error is triggered during the process, all messages of the batch will be discarded.
     *
     * @param tenant        project at the origin of the messages.
     * @param validMessages {@link BatchMessage}s that have been validated beforehand.
     */
    private void processValidMessages(String tenant, List<BatchMessage> validMessages) {
        try {
            // Launch valid message processing
            invokeBatchHandler(tenant, validMessages);
        } catch (Exception exception) { // NOSONAR
            // Re-queue all valid messages if retry is activated
            if (this.batchHandler.isRetryEnabled() && !(exception instanceof UnprocessableBatchException)) {
                executeInTransaction(() -> retryBatchMessageHandler.handleBatchMessageRetry(tenant,
                                                                                            validMessages,
                                                                                            exception), tenant);
            } else {
                executeInTransaction(() -> batchMessageErrorHandler.discardBatch(tenant, validMessages, exception),
                                     tenant);
            }
        }
    }

    /**
     * Invoke by reflection the {@link IBatchHandler#validate(Object)} method.
     *
     * @param tenant  project at the origin of the messages
     * @param message message that needs to be validated.
     * @return validation errors. Is empty if the message is valid.
     * @throws InvocationTargetException   if an unexpected error has occurred during the validation process.
     * @throws UnprocessableBatchException if a reflexion error is involved. The message will be denied because it
     *                                     cannot be processed.
     */
    protected Errors invokeValidationMethod(String tenant, Object message)
        throws InvocationTargetException, UnprocessableBatchException {
        // Prepare arguments
        Object[] arguments = new Object[] { message };
        // Invoke validation method
        try {
            runtimeTenantResolver.forceTenant(tenant);
            MethodInvoker methodInvoker = new MethodInvoker();
            methodInvoker.setTargetObject(batchHandler);
            methodInvoker.setTargetMethod(VALIDATE_SINGLE_METHOD_NAME);
            methodInvoker.setArguments(arguments);
            methodInvoker.prepare();
            return (Errors) methodInvoker.invoke();
        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException exception) {
            throw new UnprocessableBatchException(VALIDATE_SINGLE_METHOD_NAME, arguments, exception);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    /**
     * Invoke by reflection the {@link IBatchHandler#handleBatchAndLog(List, List) (Object)} method.
     *
     * @param tenant        project at the origin of the messages
     * @param validMessages message that needs to be handled.
     * @throws InvocationTargetException   if an unexpected error has occurred during the main process.
     * @throws UnprocessableBatchException if a reflexion error is involved. The batch will no longer be processed.
     */
    protected void invokeBatchHandler(String tenant, List<BatchMessage> validMessages)
        throws InvocationTargetException, UnprocessableBatchException {

        List<Message> originalMessages = new ArrayList<>();
        List<Object> convertedMessages = new ArrayList<>();
        validMessages.forEach(m -> {
            originalMessages.add(m.getOrigin());
            convertedMessages.add(m.getConverted());
        });

        // Invoke main method
        Object[] arguments = new Object[] { convertedMessages, originalMessages };
        try {
            runtimeTenantResolver.forceTenant(tenant);
            MethodInvoker methodInvoker = new MethodInvoker();
            methodInvoker.setTargetObject(batchHandler);
            methodInvoker.setTargetMethod(HANDLE_METHOD_NAME);
            methodInvoker.setArguments(arguments);
            methodInvoker.prepare();
            methodInvoker.invoke();
        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException exception) {
            throw new UnprocessableBatchException(HANDLE_METHOD_NAME, arguments, exception);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    private void acknowledgeMessages(List<Message> messages, Channel channel) {
        messages.forEach(message -> {
            try {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException e) {
                LOGGER.error("Fail to acknowledge processed message", e);
            }
        });
    }

    private void setDefaultHeaders(Message message) {
        Gson2JsonMessageConverter.setDefaultHeaders(message, batchHandler);
    }

    private void executeInTransaction(Runnable transactionalFunction, String tenant) {
        try {
            runtimeTenantResolver.forceTenant(tenant);
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {

                @Override
                public void doInTransactionWithoutResult(@NonNull TransactionStatus status) {
                    transactionalFunction.run();
                }
            });
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

}
