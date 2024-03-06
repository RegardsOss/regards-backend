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
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import org.springframework.amqp.core.MessageProperties;

/**
 * @author Iliana Ghazali
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class ResponseTestedMessage implements ISubscribable, IMessagePropertiesAware {

    private final String message;

    // Prevent GSON converter from serializing this field
    @GsonIgnore
    private MessageProperties messageProperties;

    private ResponseTestedMessage(String message) {
        this.message = message;
    }

    public static ResponseTestedMessage buildResponseMessage(String message, String requestId) {
        ResponseTestedMessage responseMessage = new ResponseTestedMessage(message);
        MessageProperties properties = new MessageProperties();
        properties.setHeader(AmqpConstants.REGARDS_REQUEST_ID_HEADER, requestId);
        responseMessage.setMessageProperties(properties);
        return responseMessage;
    }

    @Override
    public MessageProperties getMessageProperties() {
        return messageProperties;
    }

    @Override
    public void setMessageProperties(MessageProperties messageProperties) {
        this.messageProperties = messageProperties;
    }

    public String getMessage() {
        return message;
    }
}
