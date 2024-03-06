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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.batch.dto.BatchMessage;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Republish recovered messages to a dead-letter exchange (DLX) in a single transaction. If the associated AMQP handler has a dedicated DLQ, the
 * corresponding routing key will be used, otherwise the default REGARDS DLX routing key will be set.
 * Multiple message headers are added to the dead messages, as the exception stack trace stored in the header
 * 'x-exception'.
 *
 * @author Iliana Ghazali
 **/
public class RepublishErrorBatchMessageRecover extends RepublishMessageRecoverer {

    public static final String X_DEATH_QUEUE_HEADER = "x-death-queue";

    private final IPublisher publisher;

    private final IAmqpAdmin amqpAdmin;

    private final boolean dedicatedDLQEnabled;

    public RepublishErrorBatchMessageRecover(AmqpTemplate errorTemplate,
                                             IPublisher publisher,
                                             IAmqpAdmin amqpAdmin,
                                             boolean dedicatedDLQEnabled) {
        // do not set routing key as it is dynamically resolved
        super(errorTemplate, amqpAdmin.getDefaultDLXName());
        this.publisher = publisher;
        this.amqpAdmin = amqpAdmin;
        this.dedicatedDLQEnabled = dedicatedDLQEnabled;
        super.setErrorRoutingKeyPrefix("");
    }

    public void handleBatchRecover(List<BatchMessage> validMessages, Exception exception) {
        for (BatchMessage batchMessage : validMessages) {
            Message messageOrigin = batchMessage.getOrigin();
            super.recover(messageOrigin, exception);
        }
    }

    @Override
    protected void doSend(String exchange, String routingKey, Message message) {
        String tenant = message.getMessageProperties().getHeader(AmqpConstants.REGARDS_TENANT_HEADER);
        if (dedicatedDLQEnabled) {
            publisher.basicPublish(tenant,
                                   amqpAdmin.getDefaultDLXName(),
                                   amqpAdmin.getDedicatedDLRKFromQueueName(message.getMessageProperties()
                                                                                  .getConsumerQueue()),
                                   message);
        } else {
            publisher.basicPublish(tenant, amqpAdmin.getDefaultDLXName(), amqpAdmin.getDefaultDLQName(), message);
        }
    }

    @Override
    protected Map<? extends String, ?> additionalHeaders(Message message, Throwable exception) {
        Map<String, String> additionalHeaders = new HashMap<>();
        // add nested exception message if the exception was wrapped during a method call by reflexion
        Throwable cause = exception.getCause();
        if (cause instanceof InvocationTargetException invokeException) {
            additionalHeaders.put(RepublishMessageRecoverer.X_EXCEPTION_MESSAGE,
                                  invokeException.getTargetException().getMessage());
        }
        additionalHeaders.put(X_DEATH_QUEUE_HEADER, message.getMessageProperties().getConsumerQueue());
        return additionalHeaders;
    }

}
