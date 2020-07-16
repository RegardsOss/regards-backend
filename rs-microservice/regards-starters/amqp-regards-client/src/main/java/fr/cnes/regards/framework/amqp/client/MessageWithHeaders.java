/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.client;

import java.util.HashMap;

import org.springframework.amqp.core.MessageProperties;

import com.google.gson.annotations.Expose;

import fr.cnes.regards.framework.amqp.event.IMessagePropertiesAware;

/**
 * Useful to send message with headers
 * @author Marc SORDI
 */
public class MessageWithHeaders<K, V> extends HashMap<K, V> implements IMessagePropertiesAware {

    private static final long serialVersionUID = 1L;

    // Prevent GSON converter from serializing or deserializing this field
    @Expose(serialize = false, deserialize = false)
    protected MessageProperties messageProperties = new MessageProperties();

    @Override
    public void setMessageProperties(MessageProperties messageProperties) {
        this.messageProperties = messageProperties;
    }

    @Override
    public MessageProperties getMessageProperties() {
        return messageProperties;
    }

}
