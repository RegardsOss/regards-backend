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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.rabbitmq.client.Channel;
import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.converter.Gson2JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.IMessagePropertiesAware;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.notification.NotificationDtoBuilder;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareBatchMessageListener;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Batch listener
 *
 * @author Marc SORDI
 */
public class RabbitBatchMessageListener implements ChannelAwareBatchMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitBatchMessageListener.class);

    private static final String VALIDATE_SINGLE_METHOD_NAME = "validate";

    private static final String HANDLE_METHOD_NAME = "handleBatchAndLog";

    private static final String BATCH_PROCESSING_FAILURE_TITLE = "Batch processing failure";

    private static final String INVALID_MESSAGE_TITLE = "Invalid message";

    private static final String TENANT_NOT_FOUND = "Tenant cannot be found in header or message wrapper";

    private static final String CONVERSION_FAILURE_TITLE = "Conversion failure";

    // Validation error code

    private static final String INVALID_TENANT_CODE = "invalid.tenant.code";

    private static final String INVALID_MESSAGE_CODE = "invalid.message.code";

    private static final String INVALID_MESSAGE_EXCEPTION_CODE = "invalid.message.exception.code";

    // Additional headers when republishing to DLQ instead of "nacking" message to inject information

    private static final String X_EXCEPTION_MESSAGE_HEADER = "x-exception-message";

    private static final String X_EXCEPTION_STACKTRACE_HEADER = "x-exception-stacktrace";

    private final MessageConverter messageConverter;

    private final IBatchHandler<?> batchHandler;

    private final IInstancePublisher instancePublisher;

    private final IPublisher publisher;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ITenantResolver tenantResolver;

    private final String microserviceName;

    private final IAmqpAdmin amqpAdmin;

    public RabbitBatchMessageListener(IAmqpAdmin amqpAdmin,
                                      String microserviceName,
                                      IInstancePublisher instancePublisher,
                                      IPublisher publisher,
                                      IRuntimeTenantResolver runtimeTenantResolver,
                                      ITenantResolver tenantResolver,
                                      MessageConverter messageConverter,
                                      IBatchHandler<?> batchHandler) {
        this.amqpAdmin = amqpAdmin;
        this.microserviceName = microserviceName;
        this.instancePublisher = instancePublisher;
        this.publisher = publisher;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.tenantResolver = tenantResolver;
        this.messageConverter = messageConverter;
        this.batchHandler = batchHandler;
    }

    /**
     * Build a set of error string from a not empty {@link Errors} object.
     */
    private static Set<String> getErrors(Errors errors) {
        if (errors.hasErrors()) {
            Set<String> err = new HashSet<>();
            errors.getAllErrors().forEach(error -> {
                if (error instanceof FieldError fieldError) {
                    err.add(String.format("%s at %s: rejected value [%s].",
                                          fieldError.getDefaultMessage(),
                                          fieldError.getField(),
                                          ObjectUtils.nullSafeToString(fieldError.getRejectedValue())));
                } else {
                    err.add(error.getDefaultMessage());
                }
            });
            return err;
        } else {
            throw new IllegalArgumentException("This method must be called only if at least one error exists");
        }
    }

    /**
     * Build and join a set of error string from a not empty {@link Errors} object
     */
    private static String getErrorsAsString(Errors errors) {
        Set<String> errs = getErrors(errors);
        StringJoiner joiner = new StringJoiner(", ");
        errs.forEach(err -> joiner.add(err));
        return joiner.toString();
    }

    @Override
    public void onMessageBatch(List<Message> messages, Channel channel) {

        // Convert messages
        Multimap<String, BatchMessage> convertedMessages = convertMessagesByTenant(messages, channel);

        // Handle messages by tenant
        for (String tenant : convertedMessages.keySet()) {
            List<BatchMessage> validMessages = getValidMessages(convertedMessages.get(tenant), tenant, channel);
            if (!validMessages.isEmpty()) {
                try {
                    // Launch valid message processing
                    invokeBatchHandler(tenant, validMessages);
                    // Acknowledge all valid messages
                    handleValidMessage(tenant, validMessages, channel);
                } catch (Exception ex) { // NOSONAR
                    // Re-queue all valid messages
                    handleBatchException(tenant, validMessages, channel, ex);
                }
            }
        }
    }

    /**
     * Check into message headers if property x-first-death-exchange exists.
     * Property exists means message come from DLQ, so check if origin exchange match with current IBatchHandler.
     * Return true if message cannot be handled by current handler.
     *
     * @param message Message to check
     * @return boolean true if valid
     */
    private boolean handleInvalidMessageFromDLQ(BatchMessage message, String tenant, Channel channel) {
        String originExchangeName = message.getOrigin().getMessageProperties().getHeader("x-first-death-exchange");
        boolean isValid = originExchangeName == null || batchHandler.getType() == null || originExchangeName.endsWith(
            batchHandler.getType().getName());
        if (!isValid) {
            String errorMessage = String.format(
                "x-first-death-exchange found in message headers mismatch with current message %s",
                message.getClass().getName());
            handleInvalidMessage(tenant, message, channel, errorMessage);
        }
        return isValid;
    }

    /**
     * Validate message with handler delegated function
     *
     * @return true if valid
     */
    private boolean validateMessage(BatchMessage message, String tenant, Channel channel) {
        Errors errors = invokeValidationMethod(tenant, message.getConverted());
        if (errors != null && errors.hasErrors()) {
            handleInvalidMessage(tenant, message, channel, errors);
        }
        return errors == null || !errors.hasErrors();
    }

    /**
     * Concert {@link Message}s to {@link BatchMessage}s
     */
    private Multimap<String, BatchMessage> convertMessagesByTenant(List<Message> messages, Channel channel) {
        // Map of messages by tenant
        Multimap<String, BatchMessage> convertedMessages = ArrayListMultimap.create();
        // Convert messages
        for (Message message : messages) {
            try {
                setDefaultHeaders(message);
                Object converted = messageConverter.fromMessage(message);
                String tenant = message.getMessageProperties().getHeader(AmqpConstants.REGARDS_TENANT_HEADER);
                if (tenant != null && !tenant.isEmpty()) {
                    convertedMessages.put(tenant, buildBatchMessage(message, converted));
                } else {
                    handleMissingTenantError(message, channel);
                }
            } catch (MessageConversionException mce) {
                handleConversionError(message, channel, mce);
            }
        }
        return convertedMessages;
    }

    /**
     * Return valid messages from given ones.
     * Messages are validated by :
     * - associated to an active tenant
     * - If message from DLQ, origin exchange is the same as the current handler
     * - Valide message with current handler delegated function
     *
     * @param convertedMessages %essages to validate
     * @param tenant            Message tenant
     * @param channel           Amq channel
     */
    private List<BatchMessage> getValidMessages(Collection<BatchMessage> convertedMessages,
                                                String tenant,
                                                Channel channel) {
        List<BatchMessage> validMessages = new ArrayList<>();
        if (!tenantResolver.getAllActiveTenants().contains(tenant)) {
            LOGGER.debug("[AMQP MESSAGE - INVALID TENANT] Current microservice is not configured for tenant {}. {} "
                         + "messages ignored", tenant, convertedMessages.size());
            // Do not send to DLQ, it is possible that a service is not available for a given tenant. In this case,
            // just ignore messages.
        } else {
            for (BatchMessage message : convertedMessages) {
                try {
                    boolean isValid = handleInvalidMessageFromDLQ(message, tenant, channel);
                    isValid &= validateMessage(message, tenant, channel);
                    if (isValid) {
                        validMessages.add(message);
                    }
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                    handleInvalidMessage(tenant, message, channel, e.getMessage());
                }
            }
        }
        return validMessages;
    }

    protected Errors invokeValidationMethod(String tenant, Object message) {
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
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 ClassNotFoundException ex) {
            String errorMessage = "Some AMQP message has been refused as its validation failed. The message will be "
                                  + "rerouted to DLQ";
            LOGGER.error(errorMessage, ex);
            // Propagate errors
            Errors errors = new MapBindingResult(new HashMap<>(), message.getClass().getName());
            errors.reject(INVALID_MESSAGE_CODE, errorMessage);
            errors.reject(INVALID_MESSAGE_EXCEPTION_CODE, ex.getMessage());
            return errors;
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    protected void invokeBatchHandler(String tenant, List<BatchMessage> validMessages) {

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
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 ClassNotFoundException ex) {
            LOGGER.error(String.format("Fail to invoke handler %s#%s with following raw exception",
                                       batchHandler.getClass().getName(),
                                       HANDLE_METHOD_NAME), ex);
            ArrayList<String> arrayClass = new ArrayList<>();
            if (arguments != null) {
                for (Object argument : arguments) {
                    arrayClass.add(argument.getClass().toString());
                }
            }
            throw new ListenerExecutionFailedException("Fail to invoke target method '"
                                                       + HANDLE_METHOD_NAME
                                                       + "' with argument type = ["
                                                       + StringUtils.collectionToCommaDelimitedString(arrayClass)
                                                       + "], value = ["
                                                       + ObjectUtils.nullSafeToString(arguments)
                                                       + "]",
                                                       ex,
                                                       originalMessages.toArray(new Message[originalMessages.size()]));
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    private void handleValidMessage(String tenant, List<BatchMessage> validMessages, Channel channel) {
        for (BatchMessage message : validMessages) {
            try {
                channel.basicAck(message.getOrigin().getMessageProperties().getDeliveryTag(), false);
            } catch (IOException e) {
                LOGGER.error("Fail to ack valid processed message", e);
            }
        }
    }

    private void handleBatchException(String tenant, List<BatchMessage> validMessages, Channel channel, Exception ex) {

        // Message not properly wrapped! Unknown tenant!
        String errorMessage = String.format("[%s] All messages are routed to DLQ for handler %s : %s",
                                            BATCH_PROCESSING_FAILURE_TITLE,
                                            batchHandler.getClass().getName(),
                                            ex.getMessage());
        LOGGER.error(errorMessage, ex);

        // Send notification
        sendNotification(tenant, BATCH_PROCESSING_FAILURE_TITLE, errorMessage);

        for (BatchMessage message : validMessages) {

            // Reject message
            rejectMessage(tenant, message.getOrigin(), channel, BATCH_PROCESSING_FAILURE_TITLE, ex);
        }
    }

    private void handleInvalidMessage(String tenant, BatchMessage invalidMessage, Channel channel, Errors errors) {
        handleInvalidMessage(tenant, invalidMessage, channel, getErrorsAsString(errors));
    }

    private void handleInvalidMessage(String tenant,
                                      BatchMessage invalidMessage,
                                      Channel channel,
                                      String errorMessage) {
        String logMsg = String.format("[%s] For handler %s [%s] : %s",
                                      INVALID_MESSAGE_TITLE,
                                      batchHandler.getClass().getName(),
                                      errorMessage,
                                      invalidMessage.toString());

        LOGGER.error(logMsg);

        // Send notification
        sendNotification(tenant, INVALID_MESSAGE_TITLE, errorMessage);

        // Reject message
        rejectMessage(tenant,
                      invalidMessage.getOrigin(),
                      channel,
                      String.format("%s : %s", INVALID_MESSAGE_TITLE, errorMessage),
                      null);
    }

    private void handleMissingTenantError(Message message, Channel channel) {
        // Message not properly wrapped! Unknown tenant!
        String errorMessage = String.format("[%s] While preparing message for handler %s : %s",
                                            TENANT_NOT_FOUND,
                                            batchHandler.getClass().getName(),
                                            message.toString());
        LOGGER.error(errorMessage);

        // Send notification
        sendNotification(null, TENANT_NOT_FOUND, errorMessage);

        // Reject message
        rejectMessage(null, message, channel, TENANT_NOT_FOUND, null);
    }

    private void handleConversionError(Message message, Channel channel, MessageConversionException ex) {
        // Message cannot be converted
        String errorMessage = String.format("[%s] While preparing message for handler %s : %s (%s)",
                                            CONVERSION_FAILURE_TITLE,
                                            batchHandler.getClass().getName(),
                                            message.toString(),
                                            ex.getMessage());
        LOGGER.error(errorMessage, ex);

        // Send notification
        String tenant = message.getMessageProperties().getHeader(AmqpConstants.REGARDS_TENANT_HEADER);
        if (tenant != null && !tenant.isEmpty()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                if (!batchHandler.handleConversionError(message, errorMessage)) {
                    sendNotification(tenant, CONVERSION_FAILURE_TITLE, errorMessage);
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
            // Reject message
            rejectMessage(tenant, message, channel, CONVERSION_FAILURE_TITLE, ex);
        } else {
            sendNotification(null, CONVERSION_FAILURE_TITLE, errorMessage);
            // Reject message
            rejectMessage(null, message, channel, CONVERSION_FAILURE_TITLE, ex);
        }
    }

    /**
     * Reject the current message.
     *
     * @param tenant  active tenant
     * @param message {@link Message} to route to DLQ
     * @param channel {@link Channel} to use
     * @param reason  rejection reason
     * @param ex      exception thrown if any or <code>null</code>
     */
    private void rejectMessage(String tenant, Message message, Channel channel, String reason, Exception ex) {

        // If tenant is specified and for handler having dedicated DLQ,
        // trying to republish with more details overriding default behavior.
        if ((tenant != null) && batchHandler.isDedicatedDLQEnabled()) {
            // Retrieve consumer queue name
            String consumerQueueName = message.getMessageProperties().getConsumerQueue();
            // Inject header
            message.getMessageProperties().setHeader(X_EXCEPTION_MESSAGE_HEADER, reason);
            if (ex != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                message.getMessageProperties().setHeader(X_EXCEPTION_STACKTRACE_HEADER, sw.toString());
            }

            boolean republished = false;
            try {
                // Publish to DLQ
                publisher.basicPublish(tenant,
                                       amqpAdmin.getDefaultDLXName(),
                                       amqpAdmin.getDedicatedDLRKFromQueueName(consumerQueueName),
                                       message);
                republished = true;
            } catch (Exception e) {
                LOGGER.error("Fail to republish error message into dedicated DLQ", e);
            }

            // Acknowledge existing message
            try {
                if (republished) {
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                } else {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
                }
            } catch (IOException e) {
                LOGGER.error("Fail to ack message", e);
            }

        } else {

            // Route message to its DLQ without republishing
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (IOException e) {
                LOGGER.error("Fail to nack message", e);
            }
        }
    }

    /**
     * Notify project administrators on error or for unknown tenant instance user.
     *
     * @param tenant       current tenant or <code>null</code>
     * @param title        required title
     * @param errorMessage required error message
     */
    private void sendNotification(String tenant, String title, String errorMessage) {

        // Prepare notification
        NotificationDtoBuilder builder = new NotificationDtoBuilder(errorMessage,
                                                                    title,
                                                                    NotificationLevel.ERROR,
                                                                    microserviceName);
        Set<String> roles = new HashSet<>(Collections.singletonList(DefaultRole.EXPLOIT.toString()));
        NotificationEvent event = NotificationEvent.build(builder.toRoles(roles));

        if (tenant != null) {
            // Notify project
            try {
                // Route notification to the right tenant
                runtimeTenantResolver.forceTenant(tenant);
                publisher.publish(event);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        } else {
            // Notify instance
            instancePublisher.publish(event);
        }

    }

    private void setDefaultHeaders(Message message) {
        Gson2JsonMessageConverter.setDefaultHeaders(message, batchHandler);
    }

    private BatchMessage buildBatchMessage(Message origin, Object converted) {

        // Propagate message properties if required
        if (IMessagePropertiesAware.class.isAssignableFrom(converted.getClass())) {
            ((IMessagePropertiesAware) converted).setMessageProperties(origin.getMessageProperties());
        }

        BatchMessage message = new BatchMessage();
        message.setOrigin(origin);
        message.setConverted(converted);
        return message;
    }

    /**
     * Keep track of origin message
     *
     * @author Marc SORDI
     */
    private static class BatchMessage {

        private Message origin;

        private Object converted;

        public Message getOrigin() {
            return origin;
        }

        public void setOrigin(Message origin) {
            this.origin = origin;
        }

        public Object getConverted() {
            return converted;
        }

        public void setConverted(Object converted) {
            this.converted = converted;
        }
    }
}
