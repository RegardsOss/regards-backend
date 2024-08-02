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
package fr.cnes.regards.framework.amqp.test.batch.domain;

import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.event.*;
import fr.cnes.regards.framework.amqp.test.batch.mock.SimulatedMessageTypeEnum;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.AmqpHeaders;

import java.util.Objects;

/**
 * @author Marc SORDI
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class TestedMessage implements ISubscribable, IMessagePropertiesAware {

    private final SimulatedMessageTypeEnum messageType;

    // Prevent GSON converter from serializing this field
    @GsonIgnore
    protected MessageProperties messageProperties;

    private TestedMessage(SimulatedMessageTypeEnum messageType) {
        this.messageType = messageType;
        messageProperties = new MessageProperties();
        messageProperties.setHeader(AmqpConstants.REGARDS_REQUEST_ID_HEADER, RandomStringUtils.randomAlphanumeric(6));
        messageProperties.setHeader(AmqpHeaders.CORRELATION_ID, RandomStringUtils.randomAlphanumeric(4));
    }

    public static TestedMessage buildValidMessage() {
        return new TestedMessage(SimulatedMessageTypeEnum.VALID);
    }

    public static TestedMessage buildInvalidMessage() {
        return new TestedMessage(SimulatedMessageTypeEnum.INVALID);
    }

    public static TestedMessage buildEmptyTenantMessage() {
        TestedMessage testedMessage = new TestedMessage(SimulatedMessageTypeEnum.EMPTY_TENANT);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader(AmqpConstants.REGARDS_TENANT_HEADER, "");
        testedMessage.setMessageProperties(messageProperties);
        return testedMessage;
    }

    public static TestedMessage buildTemporaryUnexpectedExceptionMessage() {
        return new TestedMessage(SimulatedMessageTypeEnum.TEMPORARY_UNEXPECTED_EXCEPTION);
    }

    public static TestedMessage buildPermanentUnexpectedExceptionMessage() {
        return new TestedMessage(SimulatedMessageTypeEnum.PERMANENT_UNEXPECTED_EXCEPTION);
    }

    public SimulatedMessageTypeEnum getMessageType() {
        return messageType;
    }

    @Override
    public MessageProperties getMessageProperties() {
        return messageProperties;
    }

    @Override
    public void setMessageProperties(MessageProperties messageProperties) {
        this.messageProperties = messageProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestedMessage that = (TestedMessage) o;
        return messageType == that.messageType && Objects.equals(messageProperties, that.messageProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageType, messageProperties);
    }

    @Override
    public String toString() {
        return "TestedMessage{" + "messageType=" + messageType + ", messageProperties=" + messageProperties + '}';
    }
}
