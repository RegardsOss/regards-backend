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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.BatchMessageListener;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.util.ErrorHandler;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;

/**
 * Batch listener
 *
 * @author Marc SORDI
 *
 */
public class RabbitBatchMessageListener implements BatchMessageListener {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitBatchMessageListener.class);

    private static final String DELEGATED_METHOD_NAME = "handleBatchAndLog";

    private final MessageConverter messageConverter;

    private final IBatchHandler<?> batchHandler;

    private final ErrorHandler errorHandler;

    public RabbitBatchMessageListener(MessageConverter messageConverter, IBatchHandler<?> batchHandler,
            ErrorHandler errorHandler) {
        this.messageConverter = messageConverter;
        this.batchHandler = batchHandler;
        this.errorHandler = errorHandler;
    }

    @Override
    public void onMessageBatch(List<Message> messages) {

        // Map of original messages by tenant
        Multimap<String, Message> originaldMessages = ArrayListMultimap.create();
        // Map of converted messages by tenant
        Multimap<String, Object> convertedMessages = ArrayListMultimap.create();

        // Convert messages
        for (Message message : messages) {

            try {
                Object converted = messageConverter.fromMessage(message);
                if (TenantWrapper.class.isAssignableFrom(converted.getClass())) {
                    TenantWrapper<?> wrapper = (TenantWrapper<?>) converted;
                    originaldMessages.put(wrapper.getTenant(), message);
                    convertedMessages.put(wrapper.getTenant(), wrapper.getContent());
                } else {
                    // Delegate error handling to specified handler
                    // FIXME to test
                    errorHandler.handleError(new ListenerExecutionFailedException("Failed to convert message", null,
                            message));
                }
            } catch (MessageConversionException mce) {
                // Delegate error handling to specified handler
                // FIXME to test
                errorHandler
                        .handleError(new ListenerExecutionFailedException("Unexpected message format", null, message));
            }
        }

        // Handle messages by tenant
        for (String tenant : convertedMessages.keySet()) {
            invokeBatchHandler(buildArguments(tenant, (List<?>) convertedMessages.get(tenant)), originaldMessages
                    .get(tenant).toArray(new Message[originaldMessages.get(tenant).size()]));
        }
    }

    protected Object[] buildArguments(String tenant, List<?> messages) {
        return new Object[] { tenant, messages };
    }

    protected Object invokeBatchHandler(Object[] arguments, Message[] originalMessages) {
        try {
            MethodInvoker methodInvoker = new MethodInvoker();
            methodInvoker.setTargetObject(batchHandler);
            methodInvoker.setTargetMethod(DELEGATED_METHOD_NAME);
            methodInvoker.setArguments(arguments);
            methodInvoker.prepare();
            return methodInvoker.invoke();
        } catch (Exception ex) {
            ArrayList<String> arrayClass = new ArrayList<>();
            if (arguments != null) {
                for (Object argument : arguments) {
                    arrayClass.add(argument.getClass().toString());
                }
            }
            throw new ListenerExecutionFailedException("Failed to invoke target method '" + DELEGATED_METHOD_NAME
                    + "' with argument type = [" + StringUtils.collectionToCommaDelimitedString(arrayClass)
                    + "], value = [" + ObjectUtils.nullSafeToString(arguments) + "]", ex, originalMessages);
        }
    }
}
