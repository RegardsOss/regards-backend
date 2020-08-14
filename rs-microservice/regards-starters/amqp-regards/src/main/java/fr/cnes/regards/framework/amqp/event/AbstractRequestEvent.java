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
package fr.cnes.regards.framework.amqp.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.springframework.amqp.core.MessageProperties;

import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.event.IMessagePropertiesAware;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;

/**
 * Message headers
 *
 * @author Marc SORDI
 */
public abstract class AbstractRequestEvent implements IMessagePropertiesAware {

    // Prevent GSON converter from serializing this field
    @GsonIgnore
    @NotNull(message = "Message properties is required")
    protected MessageProperties messageProperties;

    @Override
    public MessageProperties getMessageProperties() {
        if (messageProperties == null) {
            messageProperties = new MessageProperties();
        }
        return messageProperties;
    }

    @Override
    public void setMessageProperties(MessageProperties messageProperties) {
        this.messageProperties = messageProperties;
    }

    /**
     * Generate a request ID
     */
    public static String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    public boolean hasRequestId() {
        return (getRequestId() != null) && !getRequestId().isEmpty();
    }

    public String getRequestId() {
        return getRequestId(getMessageProperties());
    }

    public static String getRequestId(MessageProperties messageProperties) {
        return messageProperties.getHeader(AmqpConstants.REGARDS_REQUEST_ID_HEADER);
    }

    public void setRequestId(String requestId) {
        getMessageProperties().setHeader(AmqpConstants.REGARDS_REQUEST_ID_HEADER, requestId);
    }

    public boolean hasRequestDate() {
        return getRequestDate() != null;
    }

    public OffsetDateTime getRequestDate() {
        return getRequestDate(getMessageProperties());
    }

    public static OffsetDateTime getRequestDate(MessageProperties messageProperties) {
        OffsetDateTime requestDate = null;
        String header = messageProperties.getHeader(AmqpConstants.REGARDS_REQUEST_DATE_HEADER);
        if (header != null) {
            requestDate = OffsetDateTimeAdapter.parse(header);
        }
        return requestDate;
    }

    public void setRequestDate(OffsetDateTime requestDate) {
        getMessageProperties().setHeader(AmqpConstants.REGARDS_REQUEST_DATE_HEADER,
                                         OffsetDateTimeAdapter.format(requestDate));
    }

    public boolean hasRequestOwner() {
        return (getRequestOwner() != null) && !getRequestOwner().isEmpty();
    }

    public String getRequestOwner() {
        return getRequestOwner(getMessageProperties());
    }

    public static String getRequestOwner(MessageProperties messageProperties) {
        return messageProperties.getHeader(AmqpConstants.REGARDS_REQUEST_OWNER_HEADER);
    }

    public void setRequestOwner(String requestOwner) {
        getMessageProperties().setHeader(AmqpConstants.REGARDS_REQUEST_OWNER_HEADER, requestOwner);
    }
}
