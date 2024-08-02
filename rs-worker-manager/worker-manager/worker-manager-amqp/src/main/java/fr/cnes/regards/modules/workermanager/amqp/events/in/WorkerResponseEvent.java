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
package fr.cnes.regards.modules.workermanager.amqp.events.in;

import fr.cnes.regards.framework.amqp.event.*;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.modules.workercommon.dto.WorkerResponse;
import fr.cnes.regards.modules.workermanager.amqp.events.EventHeadersHelper;
import org.springframework.amqp.core.MessageProperties;

import jakarta.validation.constraints.NotNull;

/**
 * Event sent by workers to inform about a request status
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON, routingKey = "#")
public class WorkerResponseEvent extends WorkerResponse implements ISubscribable, IMessagePropertiesAware {

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

    public String getRequestIdHeader() {
        return this.getMessageProperties().getHeader(EventHeadersHelper.REQUEST_ID_HEADER);
    }

}
