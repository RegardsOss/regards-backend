/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workermanager.dto.events.out;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.event.*;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.modules.workermanager.dto.events.in.RequestEvent;
import fr.cnes.regards.modules.workermanager.dto.events.EventHeaders;
import org.springframework.amqp.core.MessageProperties;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Optional;

/**
 * AMQP Event response to a {@link RequestEvent} to inform about request status.
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class ResponseEvent implements ISubscribable, IMessagePropertiesAware {

    // Prevent GSON converter from serializing this field
    @GsonIgnore
    @NotNull(message = "Message properties is required")
    protected MessageProperties messageProperties;

    private ResponseStatus status;

    private Collection<String> messages = Lists.newArrayList();

    public static ResponseEvent build(ResponseStatus status) {
        ResponseEvent event = new ResponseEvent();
        event.status = status;
        return event;
    }

    public ResponseEvent withMessage(String message) {
        this.messages.add(message);
        return this;
    }

    public ResponseEvent withMessages(Collection<String> messages) {
        this.messages.addAll(messages);
        return this;
    }

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

    public ResponseStatus getStatus() {
        return status;
    }

    public Collection<String> getMessage() {
        return messages;
    }
}
