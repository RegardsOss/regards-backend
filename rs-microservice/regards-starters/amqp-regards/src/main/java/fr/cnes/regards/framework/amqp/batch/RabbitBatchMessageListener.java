/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.rabbitmq.client.Channel;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.RabbitVersion;
import fr.cnes.regards.framework.amqp.converter.Gson2JsonMessageConverter;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.IMessagePropertiesAware;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.notification.NotificationDtoBuilder;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Batch listener
 *
 * @author Marc SORDI
 *
 */
public class RabbitBatchMessageListener implements ChannelAwareBatchMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitBatchMessageListener.class);

    private static final String VALIDATE_SINGLE_METHOD_NAME = "validate";

    private static final String HANDLE_METHOD_NAME = "handleBatchAndLog";

    private static final String BATCH_PROCESSING_FAILURE_TITLE = "Batch processing failure";

    private static final String INVALID_MESSAGE_TITLE = "Invalid message";

    private static final String UNWRAPPING_FAILURE_TITLE = "Message unwrapping failure";

    private static final String CONVERSION_FAILURE_TITLE = "Message conversion failure";

    private final MessageConverter messageConverter;

    private final IBatchHandler<?> batchHandler;

    private final IInstancePublisher instancePublisher;

    private final IPublisher publisher;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ITenantResolver tenantResolver;

    private final String microserviceName;

    public RabbitBatchMessageListener(String microserviceName, IInstancePublisher instancePublisher,
            IPublisher publisher, IRuntimeTenantResolver runtimeTenantResolver, ITenantResolver tenantResolver,
            MessageConverter messageConverter, IBatchHandler<?> batchHandler) {
        this.microserviceName = microserviceName;
        this.instancePublisher = instancePublisher;
        this.publisher = publisher;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.tenantResolver = tenantResolver;
        this.messageConverter = messageConverter;
        this.batchHandler = batchHandler;
    }

    @Override
    public void onMessageBatch(List<Message> messages, Channel channel) {

        // Map of messages by tenant
        Multimap<String, BatchMessage> convertedMessages = ArrayListMultimap.create();

        // Convert messages
        for (Message message : messages) {

            try {
                setDefaultHeaders(message);
                Object converted = messageConverter.fromMessage(message);
                if (RabbitVersion.isVersion1(message) && TenantWrapper.class.isAssignableFrom(converted.getClass())) {
                    // REGARDS API V1.0
                    TenantWrapper<?> wrapper = (TenantWrapper<?>) converted;
                    convertedMessages.put(wrapper.getTenant(), buildBatchMessage(message, wrapper.getContent()));
                } else if (RabbitVersion.isVersion1_1(message)) {
                    // REGARDS API V1.1
                    String tenant = message.getMessageProperties().getHeader(AmqpConstants.REGARDS_TENANT_HEADER);
                    convertedMessages.put(tenant, buildBatchMessage(message, converted));
                } else {
                    handleWrapperError(message, channel);
                }
            } catch (MessageConversionException mce) {
                handleConversionError(message, channel, mce);
            }
        }

        // Handle messages by tenant
        for (String tenant : convertedMessages.keySet()) {

            // Prepare message
            List<BatchMessage> validMessages = new ArrayList<>();
            for (BatchMessage message : convertedMessages.get(tenant)) {
                if (!tenantResolver.getAllActiveTenants().contains(tenant)) {
                    handleInvalidMessage(tenant, message, channel, String.format("Unkown tenant %s", tenant));
                } else if (invokeValidationMethod(tenant, message.getConverted())) {
                    validMessages.add(message);
                } else {
                    handleInvalidMessage(tenant, message, channel, "See handler validation");
                }
            }

            if (!validMessages.isEmpty()) {
                try {
                    // Launch valid message processing
                    invokeBatchHandler(tenant, validMessages);
                    // Acknowledge all valid messages
                    handleValidMessage(tenant, validMessages, channel);
                } catch (Exception ex) {
                    // Re-queue all valid messages
                    handleBatchException(tenant, validMessages, channel, ex);
                }
            }
        }
    }

    protected boolean invokeValidationMethod(String tenant, Object message) {
        // Prepare arguments
        Object[] arguments = new Object[] { tenant, message };
        // Invoke validation method
        try {
            MethodInvoker methodInvoker = new MethodInvoker();
            methodInvoker.setTargetObject(batchHandler);
            methodInvoker.setTargetMethod(VALIDATE_SINGLE_METHOD_NAME);
            methodInvoker.setArguments(arguments);
            methodInvoker.prepare();
            return (boolean) methodInvoker.invoke();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                | ClassNotFoundException ex) {
            LOGGER.error("Validation method fail - message assumed to be valid as a default fallback!", ex);
            return true;
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
        Object[] arguments = new Object[] { tenant, convertedMessages };
        invokeHandlerMethod(HANDLE_METHOD_NAME, arguments,
                            originalMessages.toArray(new Message[originalMessages.size()]));
    }

    /**
     * Invoke batch handler method by tenant
     */
    protected Object invokeHandlerMethod(String methodName, Object[] arguments, Message[] originalMessages) {
        try {
            MethodInvoker methodInvoker = new MethodInvoker();
            methodInvoker.setTargetObject(batchHandler);
            methodInvoker.setTargetMethod(methodName);
            methodInvoker.setArguments(arguments);
            methodInvoker.prepare();
            return methodInvoker.invoke();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                | ClassNotFoundException ex) {
            LOGGER.error("Fail to invoke handler with following raw exception", ex);
            ArrayList<String> arrayClass = new ArrayList<>();
            if (arguments != null) {
                for (Object argument : arguments) {
                    arrayClass.add(argument.getClass().toString());
                }
            }
            throw new ListenerExecutionFailedException("Fail to invoke target method '" + methodName
                    + "' with argument type = [" + StringUtils.collectionToCommaDelimitedString(arrayClass)
                    + "], value = [" + ObjectUtils.nullSafeToString(arguments) + "]", ex, originalMessages);
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
        String errorMessage = String.format("[%s] All messages are requeued for handler %s : %s",
                                            BATCH_PROCESSING_FAILURE_TITLE, batchHandler.getClass().getName(),
                                            ex.getMessage());
        LOGGER.error(errorMessage, ex);

        // Send notification
        sendNotification(tenant, BATCH_PROCESSING_FAILURE_TITLE, errorMessage);

        for (BatchMessage message : validMessages) {

            try {
                channel.basicNack(message.getOrigin().getMessageProperties().getDeliveryTag(), false, true);
            } catch (IOException e) {
                LOGGER.error("Fail to nack valid message with processing error", e);
            }
        }
    }

    private void handleInvalidMessage(String tenant, BatchMessage invalidMessage, Channel channel, String details) {
        // Message not properly wrapped! Unknown tenant!
        String errorMessage = String.format("[%s] For handler %s [%s] : %s", INVALID_MESSAGE_TITLE,
                                            batchHandler.getClass().getName(), details, invalidMessage.toString());
        LOGGER.error(errorMessage);

        // Send notification
        sendNotification(tenant, INVALID_MESSAGE_TITLE, errorMessage);

        // Route to DLQ
        try {
            channel.basicNack(invalidMessage.getOrigin().getMessageProperties().getDeliveryTag(), false, false);
        } catch (IOException e) {
            LOGGER.error("Fail to nack invalid message", e);
        }
    }

    private void handleWrapperError(Message message, Channel channel) {
        // Message not properly wrapped! Unknown tenant!
        String errorMessage = String.format("[%s] While preparing message for handler %s : %s",
                                            UNWRAPPING_FAILURE_TITLE, batchHandler.getClass().getName(),
                                            message.toString());
        LOGGER.error(errorMessage);

        // Send notification
        sendNotification(null, UNWRAPPING_FAILURE_TITLE, errorMessage);

        // Route to DLQ
        try {
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        } catch (IOException e) {
            LOGGER.error("Fail to nack message without tenant wrapper", e);
        }
    }

    private void handleConversionError(Message message, Channel channel, MessageConversionException ex) {
        // Message cannot be converted
        String errorMessage = String.format("[%s] While preparing message for handler %s : %s (%s)",
                                            CONVERSION_FAILURE_TITLE, batchHandler.getClass().getName(),
                                            message.toString(), ex.getMessage());
        LOGGER.error(errorMessage, ex);

        // Send notification
        if (RabbitVersion.isVersion1_1(message)) {
            String tenant = message.getMessageProperties().getHeader(AmqpConstants.REGARDS_TENANT_HEADER);
            if (batchHandler.handleConversionError(tenant, message, errorMessage)) {
                sendNotification(tenant, CONVERSION_FAILURE_TITLE, errorMessage);
            }
        } else {
            sendNotification(null, CONVERSION_FAILURE_TITLE, errorMessage);
        }

        // Route to DLQ
        try {
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        } catch (IOException e) {
            LOGGER.error("Fail to nack message that cannot be converted", e);
        }
    }

    /**
     * Notify project administrators on error or for unknown tenant instance user.
     * @param tenant current tenant or <code>null</code>
     * @param title required title
     * @param errorMessage required error message
     */
    private void sendNotification(String tenant, String title, String errorMessage) {

        // Prepare notification
        NotificationDtoBuilder builder = new NotificationDtoBuilder(errorMessage, title, NotificationLevel.ERROR,
                microserviceName);
        Set<String> roles = new HashSet<>(Arrays.asList(DefaultRole.EXPLOIT.toString()));
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
     * @author Marc SORDI
     *
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
